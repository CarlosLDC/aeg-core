package com.aeg.core.printer.credit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PrinterCreditPaymentRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,
        @NotNull(message = "paymentDate is required")
        LocalDate paymentDate,
        String reference,
        String notes,
        Boolean markAsPaid
) {
}
