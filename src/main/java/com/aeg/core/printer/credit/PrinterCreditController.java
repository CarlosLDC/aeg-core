package com.aeg.core.printer.credit;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.printer.credit.dto.PrinterCreditPaymentRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditPlanRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditSummaryResponse;

@RestController
@RequestMapping("/api/printers/{id}")
public class PrinterCreditController {

    private final PrinterCreditService service;

    public PrinterCreditController(PrinterCreditService service) {
        this.service = service;
    }

    @GetMapping("/credit-payments")
    public PrinterCreditSummaryResponse getCreditSummary(@PathVariable Long id) {
        return service.getCreditSummary(id);
    }

    @PatchMapping("/credit-plan")
    public PrinterCreditSummaryResponse updateCreditPlan(
            @PathVariable Long id,
            @Valid @RequestBody PrinterCreditPlanRequest request) {
        return service.updateCreditPlan(id, request);
    }

    @PostMapping("/credit-payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PrinterCreditSummaryResponse registerPayment(
            @PathVariable Long id,
            @Valid @RequestBody PrinterCreditPaymentRequest request) {
        return service.registerPayment(id, request);
    }

    @DeleteMapping("/credit-payments/{paymentId}")
    public PrinterCreditSummaryResponse deletePayment(
            @PathVariable Long id,
            @PathVariable Long paymentId) {
        return service.deletePayment(id, paymentId);
    }
}
