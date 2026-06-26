package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record EnajenacionTestInvoiceRequest(
        @NotNull Long printerId,
        String productDescription) {
}
