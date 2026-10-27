package com.aeg.core.mqtt.dto;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

public record EnajenacionVerifyStatusResponse(
        Long printerId,
        String fiscalSerial,
        String reportedFiscalSerial,
        boolean match,
        boolean alreadyEnajenada,
        boolean synced,
        String message,
        FiscalMqttResponseItem response) {}
