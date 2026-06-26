package com.aeg.core.enajenacion.mqtt;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityDirection;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityRecorder;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.EnajenacionTestInvoiceResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.shared.ResourceNotFoundException;

@Service
public class EnajenacionTestInvoiceService {

    private final PrinterRepository printerRepository;
    private final EnajenacionPayloadBuilder payloadBuilder;
    private final MqttService mqttService;
    private final EnajenacionActivityRecorder activityRecorder;

    public EnajenacionTestInvoiceService(
            PrinterRepository printerRepository,
            EnajenacionPayloadBuilder payloadBuilder,
            MqttService mqttService,
            EnajenacionActivityRecorder activityRecorder) {
        this.printerRepository = printerRepository;
        this.payloadBuilder = payloadBuilder;
        this.mqttService = mqttService;
        this.activityRecorder = activityRecorder;
    }

    public EnajenacionTestInvoiceResponse sendTestInvoice(Long printerId, String productDescription) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));

        if (printer.getStatus() != PrinterStatus.ENAJENADA) {
            throw new IllegalArgumentException("Solo se puede emitir factura de prueba en impresoras enajenadas.");
        }
        if (printer.getClientId() == null) {
            throw new IllegalArgumentException("La impresora no tiene cliente asignado.");
        }
        if (printer.getFiscalSerial() == null || printer.getFiscalSerial().isBlank()) {
            throw new IllegalArgumentException("La impresora no tiene serial fiscal.");
        }
        if (printer.getMacAddress() == null || printer.getMacAddress().isBlank()) {
            throw new IllegalArgumentException("La impresora no tiene dirección MAC.");
        }

        String compactMac = MacAddressNormalizer.toCompactForm(printer.getMacAddress());
        String topic = FiscalMqttTopics.comandoTopic(compactMac);
        String payload = payloadBuilder.buildInvoicePayload(productDescription);
        OffsetDateTime publishedAt = OffsetDateTime.now();

        mqttService.publish(topic, payload);
        activityRecorder.recordAdminOutbound(
                topic,
                payload,
                compactMac,
                printer.getId(),
                printer.getFiscalSerial(),
                EnajenacionActivityResult.PUBLISHED,
                "Admin test invoice");

        return new EnajenacionTestInvoiceResponse(
                topic,
                printer.getFiscalSerial(),
                printer.getMacAddress(),
                payload,
                publishedAt);
    }
}
