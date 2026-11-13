package com.aeg.core.printer.credit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PrinterCreditPaymentResponse(
        Long id,
        Long printerId,
        BigDecimal amount,
        LocalDate paymentDate,
        String reference,
        String notes,
        Long registeredById,
        String registeredByName,
        OffsetDateTime createdAt
) {
}
