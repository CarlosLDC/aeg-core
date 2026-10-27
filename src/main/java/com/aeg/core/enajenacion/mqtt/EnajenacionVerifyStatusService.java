package com.aeg.core.enajenacion.mqtt;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.EnajenacionVerifyStatusResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.SecurityScopeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnajenacionVerifyStatusService {

    private static final int VERIFY_STATUS_TIMEOUT_SECONDS = 10;
    private static final String PRINTER_QUERY_TIMEOUT_MESSAGE =
            "La impresora no respondió a tiempo. Verifique que esté encendida y conectada a la red.";

    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;
    private final EnajenacionPayloadBuilder payloadBuilder;
    private final MqttService mqttService;
    private final FiscalMqttSyncResponseAwaiter syncResponseAwaiter;
    private final FiscalResponseValidator responseValidator;
    private final EnajenacionCompletionService completionService;

    @Transactional
    public EnajenacionVerifyStatusResponse verifyAndSyncStatus(Long printerId, boolean autoSync) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        securityScope.assertPrinterInScope(printer);

        if (printer.getStatus() == PrinterStatus.ENAJENADA) {
            return new EnajenacionVerifyStatusResponse(
                    printer.getId(),
                    printer.getFiscalSerial(),
                    printer.getFiscalSerial(),
                    true,
                    true,
                    false,
                    "La impresora ya figura como Enajenada en la base de datos.",
                    null);
        }

        if (printer.getMacAddress() == null || printer.getMacAddress().isBlank()) {
            throw new IllegalArgumentException("La impresora no tiene dirección MAC registrada.");
        }

        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        String topic = FiscalMqttTopics.comandoTopic(compactMac);
        String commandPayload = payloadBuilder.buildRegistrationStatusPayload();

        CompletableFuture<FiscalMqttResponseItem> responseFuture = syncResponseAwaiter.register(
                compactMac,
                EnajenacionConstants.CMD_STA_INF);

        FiscalMqttResponseItem response;
        try {
            mqttService.publish(topic, commandPayload);
            response = responseFuture.get(VERIFY_STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            throw new EnajenacionProtocolException(PRINTER_QUERY_TIMEOUT_MESSAGE);
        } catch (Exception ex) {
            log.error("Error awaiting StaInf response for printerId={}", printerId, ex);
            throw new EnajenacionProtocolException(
                    "Error al comunicarse con la impresora vía MQTT: " + ex.getMessage());
        }

        String reportedSerial = null;
        try {
            reportedSerial = responseValidator.validateStaInfRegistrationResponse(response);
        } catch (Exception ex) {
            log.info("StaInf response does not indicate active registration for printerId={}: {}", printerId, ex.getMessage());
            return new EnajenacionVerifyStatusResponse(
                    printer.getId(),
                    printer.getFiscalSerial(),
                    null,
                    false,
                    false,
                    false,
                    "La impresora respondió pero aún no tiene registro fiscal inicializado (sigue pendiente de enajenación).",
                    response);
        }

        String expectedSerial = printer.getFiscalSerial() != null ? printer.getFiscalSerial().trim() : "";
        boolean serialMatches = expectedSerial.equalsIgnoreCase(reportedSerial);

        if (!serialMatches) {
            return new EnajenacionVerifyStatusResponse(
                    printer.getId(),
                    printer.getFiscalSerial(),
                    reportedSerial,
                    false,
                    true,
                    false,
                    "La impresora reporta el serial fiscal '" + reportedSerial + "', que no coincide con el registrado en base de datos ('" + expectedSerial + "').",
                    response);
        }

        boolean synced = false;
        if (autoSync) {
            completionService.markEnajenada(printer.getId());
            synced = true;
            log.info("Printer printerId={} verified via StaInf and successfully synchronized to ENAJENADA", printerId);
        }

        return new EnajenacionVerifyStatusResponse(
                printer.getId(),
                printer.getFiscalSerial(),
                reportedSerial,
                true,
                true,
                synced,
                synced
                        ? "La impresora ya está enajenada en hardware (serial coincidente: " + reportedSerial + "). Se sincronizó su estatus a Enajenada en base de datos."
                        : "La impresora ya está enajenada en hardware (serial coincidente: " + reportedSerial + ").",
                response);
    }
}
