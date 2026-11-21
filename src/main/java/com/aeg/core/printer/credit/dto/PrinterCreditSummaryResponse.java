package com.aeg.core.printer.credit.dto;

import java.math.BigDecimal;
import java.util.List;

import com.aeg.core.commercial.dto.CommercialPaymentResponse;
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
        List<PrinterCreditPaymentResponse> payments,
        Long facturaComercialId,
        String facturaComercialNumero,
        String facturaComercialModalidad,
        String facturaComercialEstadoCobro,
        long facturaComercialPrinterCount,
        List<CommercialPaymentResponse> commercialPayments,
        BigDecimal facturaComercialMontoTotal,
        BigDecimal facturaComercialRemainingBalance
) {
    public PrinterCreditSummaryResponse(
            Long printerId,
            String fiscalSerial,
            PrinterStatus status,
            BigDecimal finalSalePrice,
            Integer creditInstallments,
            BigDecimal totalPaid,
            BigDecimal remainingBalance,
            long paymentCount,
            boolean paid,
            List<PrinterCreditPaymentResponse> payments,
            Long facturaComercialId,
            String facturaComercialNumero,
            String facturaComercialModalidad,
            String facturaComercialEstadoCobro,
            long facturaComercialPrinterCount,
            List<CommercialPaymentResponse> commercialPayments) {
        this(printerId, fiscalSerial, status, finalSalePrice, creditInstallments,
                totalPaid, remainingBalance, paymentCount, paid, payments,
                facturaComercialId, facturaComercialNumero, facturaComercialModalidad,
                facturaComercialEstadoCobro, facturaComercialPrinterCount, commercialPayments,
                null, null);
    }

    public PrinterCreditSummaryResponse(
            Long printerId,
            String fiscalSerial,
            PrinterStatus status,
            BigDecimal finalSalePrice,
            Integer creditInstallments,
            BigDecimal totalPaid,
            BigDecimal remainingBalance,
            long paymentCount,
            boolean paid,
            List<PrinterCreditPaymentResponse> payments) {
        this(printerId, fiscalSerial, status, finalSalePrice, creditInstallments,
                totalPaid, remainingBalance, paymentCount, paid, payments,
                null, null, null, null, 0L, List.of(), null, null);
    }
}
