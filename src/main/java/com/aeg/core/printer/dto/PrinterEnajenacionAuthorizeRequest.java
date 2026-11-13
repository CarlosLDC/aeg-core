package com.aeg.core.printer.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrinterEnajenacionAuthorizeRequest(
        @NotNull(message = "creditInstallments is required")
        @Min(value = 1, message = "creditInstallments must be at least 1")
        Integer creditInstallments,
        @DecimalMin(value = "0.00", message = "finalSalePrice cannot be negative")
        BigDecimal finalSalePrice
) {
}
