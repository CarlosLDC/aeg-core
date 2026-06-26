package com.aeg.core.mqtt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.enajenacion.mqtt.AnnualInspectionMqttService;
import com.aeg.core.mqtt.dto.AnnualInspectionStaInfRequest;
import com.aeg.core.mqtt.dto.AnnualInspectionStaInfResponse;
import com.aeg.core.mqtt.dto.AnnualInspectionSubmitRequest;
import com.aeg.core.mqtt.dto.AnnualInspectionSubmitResponse;
import com.aeg.core.mqtt.dto.AnnualInspectionTestCreditNoteRequest;
import com.aeg.core.mqtt.dto.AnnualInspectionTestCreditNoteResponse;
import com.aeg.core.mqtt.dto.AnnualInspectionTestInvoiceRequest;
import com.aeg.core.mqtt.dto.AnnualInspectionTestInvoiceResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/annual-inspection")
@RequiredArgsConstructor
public class AnnualInspectionMqttAdminController {

    private final AnnualInspectionMqttService annualInspectionMqttService;

    @PostMapping("/sta-inf")
    public ResponseEntity<AnnualInspectionStaInfResponse> requestStaInf(
            @Valid @RequestBody AnnualInspectionStaInfRequest request) {
        return ResponseEntity.ok(
                annualInspectionMqttService.requestRegistrationStatus(request.printerId()));
    }

    @PostMapping("/test-invoice")
    public ResponseEntity<AnnualInspectionTestInvoiceResponse> sendTestInvoice(
            @Valid @RequestBody AnnualInspectionTestInvoiceRequest request) {
        return ResponseEntity.ok(annualInspectionMqttService.sendTestInvoice(
                request.printerId(),
                request.productDescription()));
    }

    @PostMapping("/test-credit-note")
    public ResponseEntity<AnnualInspectionTestCreditNoteResponse> sendTestCreditNote(
            @Valid @RequestBody AnnualInspectionTestCreditNoteRequest request) {
        return ResponseEntity.ok(annualInspectionMqttService.sendTestCreditNote(
                request.printerId(),
                request.numeroFacturaPrueba(),
                request.registroImpresora(),
                request.productDescription()));
    }

    @PostMapping("/submit")
    public ResponseEntity<AnnualInspectionSubmitResponse> submitInspection(
            @Valid @RequestBody AnnualInspectionSubmitRequest request) {
        return ResponseEntity.ok(annualInspectionMqttService.submitInspection(
                request.printerId(),
                request.chkPrecinto(),
                request.chkEtiquetaFiscal(),
                request.chkFactura(),
                request.chkNotaCredito(),
                request.chkSensorPapel()));
    }
}
