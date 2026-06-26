package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record AnnualInspectionTestInvoiceRequest(
        @NotNull Long printerId,
        String productDescription) {}
