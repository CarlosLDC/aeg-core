package com.aeg.core.modificationrequest.printer.dto;

import java.math.BigDecimal;

import com.aeg.core.commercial.dto.CommercialInvoiceRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrinterEnajenacionApproveRequest(
        @NotNull(message = "creditInstallments is required")
        @Min(value = 1, message = "creditInstallments must be at least 1")
        Integer creditInstallments,
        @DecimalMin(value = "0.00", message = "finalSalePrice cannot be negative")
        BigDecimal finalSalePrice,
        String reviewNote,
        Long facturaComercialId,
        @Valid CommercialInvoiceRequest newCommercialInvoice
) {
    public PrinterEnajenacionApproveRequest(
            Integer creditInstallments,
            BigDecimal finalSalePrice,
            String reviewNote) {
        this(creditInstallments, finalSalePrice, reviewNote, null, null);
    }
}
