package com.aeg.core.mqtt.dto;

import java.time.OffsetDateTime;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

public record AnnualInspectionStaInfResponse(
        String registroImpresora,
        String topic,
        String fiscalSerial,
        String macAddress,
        String commandPayload,
        FiscalMqttResponseItem response,
        OffsetDateTime publishedAt) {}
