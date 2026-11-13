package com.aeg.core.printer.credit;

import com.aeg.core.printer.credit.dto.PrinterCreditPaymentRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditPlanRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditSummaryResponse;

public interface PrinterCreditService {

    PrinterCreditSummaryResponse getCreditSummary(Long printerId);

    PrinterCreditSummaryResponse updateCreditPlan(Long printerId, PrinterCreditPlanRequest request);

    PrinterCreditSummaryResponse registerPayment(Long printerId, PrinterCreditPaymentRequest request);

    PrinterCreditSummaryResponse deletePayment(Long printerId, Long paymentId);
}
