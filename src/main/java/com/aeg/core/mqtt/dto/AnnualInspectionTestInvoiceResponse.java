package com.aeg.core.mqtt.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

public record AnnualInspectionTestInvoiceResponse(
        int numeroFacturaPrueba,
        String topic,
        String fiscalSerial,
        String macAddress,
        String commandPayload,
        List<FiscalMqttResponseItem> response,
        OffsetDateTime publishedAt) {}
