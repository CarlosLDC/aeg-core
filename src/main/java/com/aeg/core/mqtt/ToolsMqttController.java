package com.aeg.core.mqtt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.mqtt.dto.ToolsFormasPagoReadResponse;
import com.aeg.core.mqtt.dto.ToolsFormasPagoWriteRequest;
import com.aeg.core.mqtt.dto.ToolsHeaderFooterReadResponse;
import com.aeg.core.mqtt.dto.ToolsHeaderFooterWriteRequest;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.mqtt.dto.ToolsMqttStatusResponse;
import com.aeg.core.mqtt.dto.ToolsPrinterRequest;
import com.aeg.core.mqtt.dto.ToolsReportXResponse;
import com.aeg.core.mqtt.dto.ToolsReportZGetRequest;
import com.aeg.core.mqtt.dto.ToolsReportZResponse;
import com.aeg.core.mqtt.dto.ToolsReprintRequest;
import com.aeg.core.mqtt.dto.ToolsReprintResponse;
import com.aeg.core.mqtt.dto.ToolsTransmitZResponse;
import com.aeg.core.mqtt.dto.ToolsWifiConnectRequest;
import com.aeg.core.mqtt.dto.ToolsWifiScanResponse;
import com.aeg.core.tools.mqtt.ToolsMqttService;
import com.aeg.core.tools.mqtt.ToolsTestDocumentsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/tools")
@RequiredArgsConstructor
public class ToolsMqttController {

    private final ToolsMqttService toolsMqttService;
    private final ToolsTestDocumentsService toolsTestDocumentsService;

    @PostMapping("/status")
    public ResponseEntity<ToolsMqttStatusResponse> status(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.requestStatus(request.printerId()));
    }

    @PostMapping("/wifi/scan")
    public ResponseEntity<ToolsWifiScanResponse> wifiScan(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.scanWifi(request.printerId()));
    }

    @PostMapping("/wifi/connect")
    public ResponseEntity<ToolsMqttSimpleResponse> wifiConnect(
            @Valid @RequestBody ToolsWifiConnectRequest request) {
        return ResponseEntity.ok(toolsMqttService.connectWifi(
                request.printerId(), request.ssid(), request.password()));
    }

    @PostMapping("/wifi/reset")
    public ResponseEntity<ToolsMqttSimpleResponse> wifiReset(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.resetWifi(request.printerId()));
    }

    @PostMapping("/reports-z/list")
    public ResponseEntity<ToolsReportZResponse> reportsZList(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.listReportZ(request.printerId()));
    }

    @PostMapping("/reports-z/generate")
    public ResponseEntity<ToolsReportZResponse> reportsZGenerate(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.generateReportZ(request.printerId()));
    }

    @PostMapping("/reports-z/get")
    public ResponseEntity<ToolsReportZResponse> reportsZGet(@Valid @RequestBody ToolsReportZGetRequest request) {
        return ResponseEntity.ok(toolsMqttService.getReportZ(request.printerId(), request.reportNumber()));
    }

    @PostMapping("/reports-z/transmit")
    public ResponseEntity<ToolsTransmitZResponse> reportsZTransmit(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.transmitReportZ(request.printerId()));
    }

    @PostMapping("/report-x")
    public ResponseEntity<ToolsReportXResponse> reportX(@Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.reportX(request.printerId()));
    }

    @PostMapping("/formas-pago/read")
    public ResponseEntity<ToolsFormasPagoReadResponse> formasPagoRead(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.readFormasPago(request.printerId()));
    }

    @PostMapping("/formas-pago/write")
    public ResponseEntity<ToolsMqttSimpleResponse> formasPagoWrite(
            @Valid @RequestBody ToolsFormasPagoWriteRequest request) {
        return ResponseEntity.ok(toolsMqttService.writeFormasPago(
                request.printerId(), request.nroFP(), request.descripcion()));
    }

    @PostMapping("/header/read")
    public ResponseEntity<ToolsHeaderFooterReadResponse> headerRead(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.readHeader(request.printerId()));
    }

    @PostMapping("/header/write")
    public ResponseEntity<ToolsMqttSimpleResponse> headerWrite(
            @Valid @RequestBody ToolsHeaderFooterWriteRequest request) {
        return ResponseEntity.ok(toolsMqttService.writeHeader(request.printerId(), request.content()));
    }

    @PostMapping("/footer/read")
    public ResponseEntity<ToolsHeaderFooterReadResponse> footerRead(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsMqttService.readFooter(request.printerId()));
    }

    @PostMapping("/footer/write")
    public ResponseEntity<ToolsMqttSimpleResponse> footerWrite(
            @Valid @RequestBody ToolsHeaderFooterWriteRequest request) {
        return ResponseEntity.ok(toolsMqttService.writeFooter(request.printerId(), request.content()));
    }

    @PostMapping("/reprint")
    public ResponseEntity<ToolsReprintResponse> reprint(@Valid @RequestBody ToolsReprintRequest request) {
        return ResponseEntity.ok(toolsMqttService.reprint(
                request.printerId(), request.docType(), request.number(), request.mode()));
    }

    @PostMapping("/test-documents/invoice")
    public ResponseEntity<ToolsMqttSimpleResponse> testDocumentInvoice(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsTestDocumentsService.sendTestInvoice(request.printerId()));
    }

    @PostMapping("/test-documents/credit-note")
    public ResponseEntity<ToolsMqttSimpleResponse> testDocumentCreditNote(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsTestDocumentsService.sendTestCreditNote(request.printerId()));
    }

    @PostMapping("/test-documents/debit-note")
    public ResponseEntity<ToolsMqttSimpleResponse> testDocumentDebitNote(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsTestDocumentsService.sendTestDebitNote(request.printerId()));
    }

    @PostMapping("/test-documents/generate-z")
    public ResponseEntity<ToolsMqttSimpleResponse> testDocumentGenerateZ(
            @Valid @RequestBody ToolsPrinterRequest request) {
        return ResponseEntity.ok(toolsTestDocumentsService.sendTestGenerateZ(request.printerId()));
    }
}
