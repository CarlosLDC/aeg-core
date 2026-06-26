package com.aeg.core.mqtt.dto;

import java.time.OffsetDateTime;

import com.aeg.core.enajenacion.mqtt.AnnualInspectionInspAo;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

public record AnnualInspectionSubmitResponse(
        long dataTimestamp,
        AnnualInspectionInspAo inspAo,
        String topic,
        String fiscalSerial,
        String macAddress,
        String commandPayload,
        FiscalMqttResponseItem response,
        OffsetDateTime publishedAt) {}
