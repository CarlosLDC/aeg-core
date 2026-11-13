package com.aeg.core.modificationrequest.printer;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionApproveRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionCreateRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRejectRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRequestDetailResponse;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRequestListItemResponse;

@RestController
@RequestMapping("/api/printer-enajenacion-requests")
public class PrinterEnajenacionRequestController {

    private final PrinterEnajenacionRequestService service;

    public PrinterEnajenacionRequestController(PrinterEnajenacionRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrinterEnajenacionRequestDetailResponse create(
            @Valid @RequestBody PrinterEnajenacionCreateRequest request) {
        return service.createRequest(request);
    }

    @GetMapping
    public List<PrinterEnajenacionRequestListItemResponse> findByStatus(
            @RequestParam(defaultValue = "PENDING") ModificationRequestStatus status) {
        return service.findByStatus(status);
    }

    @GetMapping("/pending")
    public ResponseEntity<PrinterEnajenacionRequestDetailResponse> findPendingByPrinterId(
            @RequestParam Long printerId) {
        return service.findPendingByPrinterId(printerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public PrinterEnajenacionRequestDetailResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/approve")
    public PrinterEnajenacionRequestDetailResponse approve(
            @PathVariable Long id,
            @Valid @RequestBody PrinterEnajenacionApproveRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    public PrinterEnajenacionRequestDetailResponse reject(
            @PathVariable Long id,
            @RequestBody(required = false) PrinterEnajenacionRejectRequest request) {
        return service.reject(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PrinterEnajenacionRequestDetailResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
