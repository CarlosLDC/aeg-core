package com.aeg.core.tools.mqtt;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter;
import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolsTestDocumentsService {

    private static final String TIMEOUT_MESSAGE =
            "La impresora no respondió a tiempo. Verifique que esté encendida y conectada a la red.";
    private static final int COMMAND_DELAY_MS = 50;

    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;
    private final MqttService mqttService;
    private final FiscalMqttSyncResponseAwaiter syncResponseAwaiter;
    private final ToolsTestDocumentsPayloadBuilder payloadBuilder;
    private final ToolsMqttSettings settings;

    public ToolsMqttSimpleResponse sendTestInvoice(Long printerId) {
        return publishSequence(
                printerId,
                payloadBuilder.testInvoicePayloads(),
                ToolsMqttConstants.CMD_END_FAC,
                settings.testInvoiceTimeoutSeconds(),
                "Factura de prueba generada correctamente.");
    }

    public ToolsMqttSimpleResponse sendTestCreditNote(Long printerId) {
        String serial = requireFiscalSerial(printerId);
        return publishSequence(
                printerId,
                payloadBuilder.testCreditNotePayloads(serial),
                ToolsMqttConstants.CMD_END_NC,
                settings.testNoteTimeoutSeconds(),
                "Nota de crédito de prueba generada correctamente.");
    }

    public ToolsMqttSimpleResponse sendTestDebitNote(Long printerId) {
        String serial = requireFiscalSerial(printerId);
        return publishSequence(
                printerId,
                payloadBuilder.testDebitNotePayloads(serial),
                ToolsMqttConstants.CMD_END_ND,
                settings.testNoteTimeoutSeconds(),
                "Nota de débito de prueba generada correctamente.");
    }

    public ToolsMqttSimpleResponse sendTestGenerateZ(Long printerId) {
        return publishSequence(
                printerId,
                payloadBuilder.testGenerateZPayloads(),
                ToolsMqttConstants.CMD_GEN_IMP_REP_Z,
                settings.testGenerateZTimeoutSeconds(),
                "Reporte Z de prueba generado correctamente.");
    }

    private ToolsMqttSimpleResponse publishSequence(
            Long printerId,
            List<String> payloads,
            String terminalCmd,
            int timeoutSeconds,
            String successMessage) {
        PrinterContext ctx = resolvePrinter(printerId);
        CompletableFuture<FiscalMqttResponseItem> future =
                syncResponseAwaiter.register(ctx.compactMac(), terminalCmd);
        try {
            for (int i = 0; i < payloads.size(); i++) {
                mqttService.publish(ctx.topic(), payloads.get(i));
                if (i < payloads.size() - 1) {
                    Thread.sleep(COMMAND_DELAY_MS);
                }
            }
            FiscalMqttResponseItem response = future.get(timeoutSeconds, TimeUnit.SECONDS);
            if (response.code() != null && response.code() == 0) {
                return ToolsMqttSimpleResponse.ok(successMessage);
            }
            String message = response.dataS() != null && !response.dataS().isBlank()
                    ? response.dataS()
                    : "Error de impresora (code: " + response.code() + ")";
            return ToolsMqttSimpleResponse.error(message);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(TIMEOUT_MESSAGE);
        } catch (ExecutionException ex) {
            throw wrapExecution(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("No se pudo completar el documento de prueba.");
        } finally {
            syncResponseAwaiter.cancel(ctx.compactMac());
        }
    }

    private String requireFiscalSerial(Long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        securityScope.assertPrinterInScope(printer);
        if (printer.getFiscalSerial() == null || printer.getFiscalSerial().isBlank()) {
            throw new EnajenacionProtocolException(
                    "La impresora no tiene serial fiscal registrado. Regístrelo antes de generar notas de prueba.");
        }
        return printer.getFiscalSerial();
    }

    private RuntimeException wrapExecution(ExecutionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        return new EnajenacionProtocolException("No se pudo completar el documento de prueba.");
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

    private record PrinterContext(String compactMac, String topic) {}
}
