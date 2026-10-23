package com.aeg.core.tools.mqtt;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

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
import com.aeg.core.mqtt.dto.ToolsReportXResponse;
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
    private final BrokerMigrationLegacyClient legacyClient;
    private final ConcurrentHashMap<String, ReentrantLock> locksByMac = new ConcurrentHashMap<>();

    public ToolsMqttStatusResponse requestStatus(Long printerId) {
        StatusBrokerReply reply = publishAndAwaitOnAnyBroker(
                printerId,
                payloadBuilder.statusPayload(),
                ToolsMqttResponseParser::isStatusResponse,
                settings.statusTimeoutSeconds());
        return responseParser.parseStatus(reply.item()).withLegacyBroker(reply.legacyBroker());
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
        publishFireAndForgetOnAnyBroker(ctx, payloadBuilder.wifiResetPayload());
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

    public ToolsReportXResponse reportX(Long printerId) {
        return reportX(printerId, "visualize");
    }

    /**
     * TODO(tools-report-x): incomplete — visualize still uses impRepX and may print.
     * Awaiting a dedicated MQTT command for preview-without-print; then wire it here.
     */
    public ToolsReportXResponse reportX(Long printerId, String mode) {
        boolean printPhysically = mode != null && "print".equalsIgnoreCase(mode.trim());
        if (printPhysically) {
            FiscalMqttResponseItem response = publishAndAwait(
                    printerId,
                    payloadBuilder.reportXPayload(true),
                    ToolsMqttConstants.CMD_IMP_REP_X,
                    settings.defaultTimeoutSeconds());
            responseParser.parseSimpleAck(response, "Error al imprimir el reporte X");
            return ToolsReportXResponse.printed();
        }

        ToolsMqttTextChunksResult chunksResult = publishAndAwaitTextChunks(
                printerId,
                payloadBuilder.reportXPayload(false),
                ToolsMqttConstants.CMD_IMP_REP_X,
                settings.reprintTimeoutSeconds());
        if (chunksResult.terminal().code() != 0) {
            throw new ToolsMqttOperationException(
                    "La impresora rechazó la generación del reporte X (código "
                            + chunksResult.terminal().code()
                            + ").");
        }
        String escPos = responseParser.parseReprintChunks(chunksResult.chunks());
        if (escPos.isBlank()) {
            throw new EnajenacionProtocolException("La impresora no devolvió contenido del reporte X.");
        }
        return ToolsReportXResponse.ok(escPos);
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
            List<String> lines = ToolsHeaderFooterPayloadSupport.parseLines(content);
            ToolsHeaderFooterPayloadSupport.validateLines(
                    lines, ToolsMqttConstants.HEADER_MAX_LINES, "encabezado");
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
            List<String> lines = ToolsHeaderFooterPayloadSupport.parseLines(content);
            ToolsHeaderFooterPayloadSupport.validateLines(
                    lines, ToolsMqttConstants.FOOTER_MAX_LINES, "pie de página");
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
        String tipoRe = mapReprintTipoRe(docType);
        int docNumber = number != null ? number : 0;
        if (docNumber <= 0) {
            throw new ToolsMqttOperationException("Indique un número de documento válido.");
        }
        String normalizedMode = mode != null ? mode : "visualize";

        if ("reprint".equalsIgnoreCase(normalizedMode)) {
            publishAndAwait(
                    printerId,
                    payloadBuilder.reprintPayload(tipoRe, docNumber, true),
                    ToolsMqttConstants.CMD_REIM_REP,
                    settings.defaultTimeoutSeconds());
            return ToolsReprintResponse.ack(normalizedMode, docType, number);
        }

        ToolsMqttTextChunksResult chunksResult = publishAndAwaitTextChunks(
                printerId,
                payloadBuilder.reprintPayload(tipoRe, docNumber, false),
                ToolsMqttConstants.CMD_REIM_REP,
                settings.reprintTimeoutSeconds());
        if (chunksResult.terminal().code() != 0) {
            throw new ToolsMqttOperationException(
                    "La impresora rechazó la visualización del documento (código "
                            + chunksResult.terminal().code()
                            + ").");
        }
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
        return executeOnAnyBroker(
                printerId,
                ctx -> syncResponseAwaiter.register(ctx.compactMac(), expectedCmd),
                ctx -> mqttService.publish(ctx.topic(), payload),
                ctx -> legacyClient.publishAndAwaitItem(ctx.compactMac(), payload, expectedCmd, timeoutSeconds),
                timeoutSeconds).item();
    }

    private StatusBrokerReply publishAndAwaitOnAnyBroker(
            Long printerId,
            String payload,
            Predicate<FiscalMqttResponseItem> matcher,
            int timeoutSeconds) {
        BrokerReply<FiscalMqttResponseItem> reply = executeOnAnyBroker(
                printerId,
                ctx -> syncResponseAwaiter.registerWithMatcher(ctx.compactMac(), matcher),
                ctx -> mqttService.publish(ctx.topic(), payload),
                ctx -> legacyClient.publishAndAwaitItem(ctx.compactMac(), payload, matcher, timeoutSeconds),
                timeoutSeconds);
        return new StatusBrokerReply(reply.item(), reply.legacyBroker());
    }

    private FiscalMqttResponseItem publishAndAwaitMatcher(
            Long printerId,
            String payload,
            Predicate<FiscalMqttResponseItem> matcher,
            int timeoutSeconds) {
        return publishAndAwaitOnAnyBroker(printerId, payload, matcher, timeoutSeconds).item();
    }

    private ToolsMqttTextChunksResult publishAndAwaitTextChunks(
            Long printerId,
            String payload,
            String terminalCmd,
            int timeoutSeconds) {
        return executeOnAnyBroker(
                printerId,
                ctx -> syncResponseAwaiter.registerTextChunks(ctx.compactMac(), terminalCmd),
                ctx -> mqttService.publish(ctx.topic(), payload),
                ctx -> legacyClient.publishAndAwaitTextChunks(ctx.compactMac(), payload, terminalCmd, timeoutSeconds),
                timeoutSeconds).item();
    }

    private void publishFireAndForgetOnAnyBroker(PrinterContext ctx, String payload) {
        RuntimeException mainError = null;
        try {
            mqttService.publish(ctx.topic(), payload);
        } catch (RuntimeException ex) {
            mainError = ex;
            log.warn("Failed to publish to main broker for {}: {}", ctx.compactMac(), ex.getMessage());
        }

        RuntimeException legacyError = null;
        try {
            legacyClient.publishFireAndForget(ctx.compactMac(), payload);
        } catch (RuntimeException ex) {
            legacyError = ex;
            log.debug("Failed to publish to legacy broker for {}: {}", ctx.compactMac(), ex.getMessage());
        }

        if (mainError != null && legacyError != null) {
            throw mainError;
        }
    }

    private <T> BrokerReply<T> executeOnAnyBroker(
            Long printerId,
            Function<PrinterContext, CompletableFuture<T>> newBrokerRegister,
            Consumer<PrinterContext> newBrokerPublish,
            Function<PrinterContext, T> legacyBrokerCall,
            int timeoutSeconds) {
        PrinterContext ctx = resolvePrinter(printerId);
        ReentrantLock lock = lockFor(ctx.compactMac());
        lock.lock();
        try {
            CompletableFuture<T> registeredFuture = newBrokerRegister.apply(ctx);
            CompletableFuture<T> newBrokerFuture =
                    registeredFuture != null ? registeredFuture : new CompletableFuture<>();
            CompletableFuture<T> legacyBrokerFuture = new CompletableFuture<>();
            CompletableFuture<BrokerReply<T>> winnerFuture = new CompletableFuture<>();

            newBrokerFuture.whenComplete((item, ex) -> {
                if (ex == null && item != null) {
                    winnerFuture.complete(new BrokerReply<>(item, false));
                } else if (legacyBrokerFuture.isDone()) {
                    completeWhenBothUnsuccessful(winnerFuture, ex, failureCause(legacyBrokerFuture));
                }
            });

            legacyBrokerFuture.whenComplete((item, ex) -> {
                if (ex == null && item != null) {
                    winnerFuture.complete(new BrokerReply<>(item, true));
                } else if (newBrokerFuture.isDone()) {
                    completeWhenBothUnsuccessful(winnerFuture, failureCause(newBrokerFuture), ex);
                }
            });

            Thread legacyThread = Thread.ofVirtual()
                    .name("tools-legacy-" + ctx.compactMac())
                    .start(() -> {
                        try {
                            T item = legacyBrokerCall.apply(ctx);
                            if (item != null) {
                                legacyBrokerFuture.complete(item);
                            } else {
                                legacyBrokerFuture.completeExceptionally(
                                        new BrokerMigrationLegacyClient.LegacyBrokerTimeoutException(
                                                "Sin respuesta en broker antiguo"));
                            }
                        } catch (Throwable t) {
                            legacyBrokerFuture.completeExceptionally(t);
                        }
                    });

            try {
                try {
                    newBrokerPublish.accept(ctx);
                } catch (Throwable publishEx) {
                    newBrokerFuture.completeExceptionally(publishEx);
                }
                return winnerFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                throw new EnajenacionProtocolException(TIMEOUT_MESSAGE);
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof TimeoutException
                        || ex.getCause() instanceof BrokerMigrationLegacyClient.LegacyBrokerTimeoutException) {
                    throw new EnajenacionProtocolException(TIMEOUT_MESSAGE);
                }
                throw wrapExecution(ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new EnajenacionProtocolException(ERROR_MESSAGE);
            } finally {
                syncResponseAwaiter.cancel(ctx.compactMac());
                legacyThread.interrupt();
            }
        } finally {
            lock.unlock();
        }
    }

    private static <T> void completeWhenBothUnsuccessful(
            CompletableFuture<BrokerReply<T>> winnerFuture,
            Throwable primaryError,
            Throwable secondaryError) {
        if (primaryError != null) {
            winnerFuture.completeExceptionally(primaryError);
        } else if (secondaryError != null) {
            winnerFuture.completeExceptionally(secondaryError);
        } else {
            winnerFuture.completeExceptionally(new TimeoutException());
        }
    }

    private static Throwable failureCause(CompletableFuture<?> future) {
        try {
            future.getNow(null);
            return null;
        } catch (Throwable t) {
            return t.getCause() != null ? t.getCause() : t;
        }
    }

    private ReentrantLock lockFor(String compactMac) {
        return locksByMac.computeIfAbsent(compactMac, ignored -> new ReentrantLock());
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

    private static String mapReprintTipoRe(String docType) {
        if (docType == null || docType.isBlank()) {
            return "rFactura";
        }
        String normalized = docType.trim();
        if (normalized.startsWith("r") && normalized.length() > 1) {
            return normalized;
        }
        return switch (normalized.toUpperCase()) {
            case "FACTURA", "FAC" -> "rFactura";
            case "NOTA_CREDITO", "NC" -> "rNotCre";
            case "NOTA_DEBITO", "ND" -> "rNotDeb";
            case "NO_FISCAL", "NF", "DOCUMENTO-NO-FISCAL", "DOCUMENTO_NO_FISCAL" -> "rDoNFis";
            case "Z", "REPORTE_Z", "REPZ", "REP_Z" -> "rReporZ";
            default -> throw new ToolsMqttOperationException(
                    "Tipo de documento no soportado para reimpresión: " + docType);
        };
    }

    private record PrinterContext(String compactMac, String topic) {}

    private record BrokerReply<T>(T item, boolean legacyBroker) {}

    private record StatusBrokerReply(FiscalMqttResponseItem item, boolean legacyBroker) {}
}
