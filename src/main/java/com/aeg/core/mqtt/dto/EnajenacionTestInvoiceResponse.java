package com.aeg.core.mqtt.dto;

import java.time.OffsetDateTime;

public record EnajenacionTestInvoiceResponse(
        String topic,
        String fiscalSerial,
        String mac,
        String payload,
        OffsetDateTime publishedAt) {
}
