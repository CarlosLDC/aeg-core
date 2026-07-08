package com.aeg.core.tools.mqtt;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter.ToolsMqttTextChunksResult;
import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.ToolsFormasPagoItemDto;
import com.aeg.core.mqtt.dto.ToolsFormasPagoReadResponse;
import com.aeg.core.mqtt.dto.ToolsHeaderFooterReadResponse;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.mqtt.dto.ToolsMqttStatusResponse;
import com.aeg.core.mqtt.dto.ToolsReportZDataDto;
import com.aeg.core.mqtt.dto.ToolsReportZResponse;
import com.aeg.core.mqtt.dto.ToolsReprintResponse;
import com.aeg.core.mqtt.dto.ToolsTransmitZResponse;
import com.aeg.core.mqtt.dto.ToolsWifiNetworkDto;
import com.aeg.core.mqtt.dto.ToolsWifiScanResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolsMqttService {

    private static final String TIMEOUT_MESSAGE =
            "La impresora no respondió a tiempo. Verifique que esté encendida y conectada a la red.";
    private static final String ERROR_MESSAGE =
            "No se pudo completar la operación MQTT con la impresora.";

    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;
    private final MqttService mqttService;
    private final FiscalMqttSyncResponseAwaiter syncResponseAwaiter;
    private final ToolsMqttPayloadBuilder payloadBuilder;
    private final ToolsMqttResponseParser responseParser;
    private final ToolsMqttSettings settings;

    public ToolsMqttStatusResponse requestStatus(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwaitMatcher(
                printerId,
                payloadBuilder.statusPayload(),
                ToolsMqttResponseParser::isStatusResponse,
                settings.statusTimeoutSeconds());
        return responseParser.parseStatus(response);
    }

    public ToolsWifiScanResponse scanWifi(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwaitMatcher(
                printerId,
                payloadBuilder.wifiScanPayload(),
                ToolsMqttResponseParser::isWifiScanResponse,
                settings.wifiScanTimeoutSeconds());
        List<ToolsWifiNetworkDto> networks = responseParser.parseWifiScan(response);
        return ToolsWifiScanResponse.ok(networks);
    }

    public ToolsMqttSimpleResponse connectWifi(Long printerId, String ssid, String password) {
        try {
            FiscalMqttResponseItem response = publishAndAwait(
                    printerId,
                    payloadBuilder.wifiConnectPayload(ssid, password),
                    ToolsMqttConstants.CMD_WIFI_CONF,
                    settings.wifiTimeoutSeconds());
            responseParser.parseWifiConnect(response);
            return ToolsMqttSimpleResponse.ok("Conexión WiFi enviada correctamente.");
        } catch (ToolsMqttOperationException ex) {
            return ToolsMqttSimpleResponse.error(ex.getMessage());
        }
    }

    public ToolsMqttSimpleResponse resetWifi(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        try {
            mqttService.publish(ctx.topic(), payloadBuilder.wifiResetPayload());
        } catch (JsonProcessingException ex) {
            throw new EnajenacionProtocolException("No se pudo construir el comando de reinicio.");
        }
        return ToolsMqttSimpleResponse.ok("Comando de reinicio enviado a la impresora.");
    }

    public ToolsReportZResponse listReportZ(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwait(
                printerId,
                payloadBuilder.listReportZPayload(),
                ToolsMqttConstants.CMD_GET_REP_Z,
                settings.reportZTimeoutSeconds());
        ToolsReportZDataDto report = responseParser.parseReportZ(response);
        return ToolsReportZResponse.ok(report);
    }

    public ToolsReportZResponse generateReportZ(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwait(
                printerId,
                payloadBuilder.generateReportZPayload(),
                ToolsMqttConstants.CMD_REP_Z,
                settings.reportZTimeoutSeconds());
        ToolsReportZDataDto report = responseParser.parseReportZ(response);
        return ToolsReportZResponse.ok(report);
    }

    public ToolsReportZResponse getReportZ(Long printerId, int reportNumber) {
        FiscalMqttResponseItem response = publishAndAwait(
                printerId,
                payloadBuilder.getReportZPayload(reportNumber),
                ToolsMqttConstants.CMD_GET_REP_Z,
                settings.reportZTimeoutSeconds());
        ToolsReportZDataDto report = responseParser.parseReportZ(response);
        return ToolsReportZResponse.ok(report);
    }

    public ToolsTransmitZResponse transmitReportZ(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwaitMatcher(
                printerId,
                payloadBuilder.lastTransmittedZPayload(),
                ToolsMqttResponseParser::isLastTransmittedZResponse,
                settings.reportZTimeoutSeconds());
        return responseParser.parseTransmitZ(response);
    }

    public ToolsMqttSimpleResponse reportX(Long printerId) {
        publishAndAwait(
                printerId,
                payloadBuilder.reportXPayload(),
                ToolsMqttConstants.CMD_IMP_REP_X,
                settings.defaultTimeoutSeconds());
        return ToolsMqttSimpleResponse.ok("Reporte X enviado a la impresora.");
    }

    public ToolsFormasPagoReadResponse readFormasPago(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwaitMatcher(
                printerId,
                payloadBuilder.formasPagoReadPayload(),
                ToolsMqttResponseParser::isFormasPagoResponse,
                settings.formasPagoTimeoutSeconds());
        List<ToolsFormasPagoItemDto> items = responseParser.parseFormasPago(response);
        return ToolsFormasPagoReadResponse.ok(items);
    }

    public ToolsMqttSimpleResponse writeFormasPago(Long printerId, int nroFp, String descripcion) {
        try {
            FiscalMqttResponseItem response = publishAndAwait(
                    printerId,
                    payloadBuilder.formasPagoWritePayload(nroFp, descripcion),
                    ToolsMqttConstants.CMD_DESC_FP,
                    settings.formasPagoTimeoutSeconds());
            responseParser.parseSimpleAck(response, "Error al actualizar forma de pago");
            return ToolsMqttSimpleResponse.ok("Forma de pago actualizada.");
        } catch (ToolsMqttOperationException ex) {
            return ToolsMqttSimpleResponse.error(ex.getMessage());
        }
    }

    public ToolsHeaderFooterReadResponse readHeader(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwaitMatcher(
                printerId,
                payloadBuilder.headerReadPayload(),
                ToolsMqttResponseParser::isHeaderFooterReadResponse,
                settings.defaultTimeoutSeconds());
        return ToolsHeaderFooterReadResponse.ok(responseParser.parseHeaderFooter(response));
    }

    public ToolsMqttSimpleResponse writeHeader(Long printerId, String content) {
        try {
            FiscalMqttResponseItem response = publishAndAwait(
                    printerId,
                    payloadBuilder.headerWritePayload(content),
                    ToolsMqttConstants.CMD_W_FILE_SPIFF,
                    settings.defaultTimeoutSeconds());
            responseParser.parseSimpleAck(response, "Error al escribir encabezado");
            return ToolsMqttSimpleResponse.ok("Encabezado actualizado.");
        } catch (ToolsMqttOperationException ex) {
            return ToolsMqttSimpleResponse.error(ex.getMessage());
        }
    }

    public ToolsHeaderFooterReadResponse readFooter(Long printerId) {
        FiscalMqttResponseItem response = publishAndAwaitMatcher(
                printerId,
                payloadBuilder.footerReadPayload(),
                ToolsMqttResponseParser::isHeaderFooterReadResponse,
                settings.defaultTimeoutSeconds());
        return ToolsHeaderFooterReadResponse.ok(responseParser.parseHeaderFooter(response));
    }

    public ToolsMqttSimpleResponse writeFooter(Long printerId, String content) {
        try {
            FiscalMqttResponseItem response = publishAndAwait(
                    printerId,
                    payloadBuilder.footerWritePayload(content),
                    ToolsMqttConstants.CMD_PIE_TI_F,
                    settings.defaultTimeoutSeconds());
            responseParser.parseSimpleAck(response, "Error al escribir pie de página");
            return ToolsMqttSimpleResponse.ok("Pie de página actualizado.");
        } catch (ToolsMqttOperationException ex) {
            return ToolsMqttSimpleResponse.error(ex.getMessage());
        }
    }

    public ToolsReprintResponse reprint(Long printerId, String docType, Integer number, String mode) {
        String mappedType = mapReprintDocType(docType);
        int docNumber = number != null ? number : 0;
        String normalizedMode = mode != null ? mode : "visualize";

        if ("reprint".equalsIgnoreCase(normalizedMode)) {
            publishAndAwait(
                    printerId,
                    payloadBuilder.reprintPayload(mappedType, docNumber),
                    ToolsMqttConstants.CMD_REIM_REP,
                    settings.defaultTimeoutSeconds());
            return ToolsReprintResponse.ack(normalizedMode, docType, number);
        }

        ToolsMqttTextChunksResult chunksResult = publishAndAwaitTextChunks(
                printerId,
                payloadBuilder.reprintPayload(mappedType, docNumber),
                ToolsMqttConstants.CMD_REIM_REP,
                settings.reprintTimeoutSeconds());
        String escPos = responseParser.parseReprintChunks(chunksResult.chunks());
        if (escPos.isBlank()) {
            throw new EnajenacionProtocolException("La impresora no devolvió contenido del documento.");
        }
        return ToolsReprintResponse.ok(escPos, normalizedMode, docType, number);
    }

    private FiscalMqttResponseItem publishAndAwait(
            Long printerId,
            String payload,
            String expectedCmd,
            int timeoutSeconds) {
        PrinterContext ctx = resolvePrinter(printerId);
        CompletableFuture<FiscalMqttResponseItem> future =
                syncResponseAwaiter.register(ctx.compactMac(), expectedCmd);
        return await(ctx, payload, future, timeoutSeconds);
    }

    private FiscalMqttResponseItem publishAndAwaitMatcher(
            Long printerId,
            String payload,
            Predicate<FiscalMqttResponseItem> matcher,
            int timeoutSeconds) {
        PrinterContext ctx = resolvePrinter(printerId);
        CompletableFuture<FiscalMqttResponseItem> future =
                syncResponseAwaiter.registerWithMatcher(ctx.compactMac(), matcher);
        return await(ctx, payload, future, timeoutSeconds);
    }

    private ToolsMqttTextChunksResult publishAndAwaitTextChunks(
            Long printerId,
            String payload,
            String terminalCmd,
            int timeoutSeconds) {
        PrinterContext ctx = resolvePrinter(printerId);
        CompletableFuture<ToolsMqttTextChunksResult> future =
                syncResponseAwaiter.registerTextChunks(ctx.compactMac(), terminalCmd);
        try {
            mqttService.publish(ctx.topic(), payload);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(TIMEOUT_MESSAGE);
        } catch (ExecutionException ex) {
            throw wrapExecution(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException(ERROR_MESSAGE);
        } finally {
            syncResponseAwaiter.cancel(ctx.compactMac());
        }
    }

    private FiscalMqttResponseItem await(
            PrinterContext ctx,
            String payload,
            CompletableFuture<FiscalMqttResponseItem> future,
            int timeoutSeconds) {
        try {
            mqttService.publish(ctx.topic(), payload);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(TIMEOUT_MESSAGE);
        } catch (ExecutionException ex) {
            throw wrapExecution(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException(ERROR_MESSAGE);
        } finally {
            syncResponseAwaiter.cancel(ctx.compactMac());
        }
    }

    private RuntimeException wrapExecution(ExecutionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        return new EnajenacionProtocolException(ERROR_MESSAGE);
    }

    private PrinterContext resolvePrinter(Long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        securityScope.assertPrinterInScope(printer);
        if (printer.getMacAddress() == null || printer.getMacAddress().isBlank()) {
            throw new EnajenacionProtocolException(
                    "La impresora no tiene dirección MAC registrada. Regístrela antes de usar operaciones MQTT.");
        }
        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        return new PrinterContext(compactMac, FiscalMqttTopics.comandoTopic(compactMac));
    }

    private static String mapReprintDocType(String docType) {
        if (docType == null || docType.isBlank()) {
            return "FAC";
        }
        return switch (docType.trim().toUpperCase()) {
            case "FACTURA", "FAC" -> "FAC";
            case "NOTA_CREDITO", "NC" -> "NC";
            case "NOTA_DEBITO", "ND" -> "ND";
            case "Z" -> "Z";
            default -> docType.trim().toUpperCase();
        };
    }

    private record PrinterContext(String compactMac, String topic) {}
}
