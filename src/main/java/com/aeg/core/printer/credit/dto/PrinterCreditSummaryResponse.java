package com.aeg.core.printer.credit.dto;

import java.math.BigDecimal;
import java.util.List;

import com.aeg.core.printer.PrinterStatus;

public record PrinterCreditSummaryResponse(
        Long printerId,
        String fiscalSerial,
        PrinterStatus status,
        BigDecimal finalSalePrice,
        Integer creditInstallments,
        BigDecimal totalPaid,
        BigDecimal remainingBalance,
        long paymentCount,
        boolean paid,
        List<PrinterCreditPaymentResponse> payments
) {
}
