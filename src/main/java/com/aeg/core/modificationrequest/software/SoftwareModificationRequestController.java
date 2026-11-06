package com.aeg.core.modificationrequest.software;

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
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationCreateRequest;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestListItemResponse;

@RestController
@RequestMapping("/api/software-modification-requests")
public class SoftwareModificationRequestController {

    private final SoftwareModificationRequestService service;

    public SoftwareModificationRequestController(SoftwareModificationRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SoftwareModificationRequestDetailResponse createRequest(
            @Valid @RequestBody SoftwareModificationCreateRequest request) {
        return service.createRequest(request);
    }

    @GetMapping
    public List<SoftwareModificationRequestListItemResponse> findByStatus(
            @RequestParam(required = false) ModificationRequestStatus status) {
        return service.findByStatus(status);
    }

    @GetMapping("/pending")
    public ResponseEntity<SoftwareModificationRequestDetailResponse> findPendingByPrinterId(
            @RequestParam Long printerId) {
        return service.findPendingByPrinterId(printerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public SoftwareModificationRequestDetailResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/approve")
    public SoftwareModificationRequestDetailResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public SoftwareModificationRequestDetailResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/cancel")
    public SoftwareModificationRequestDetailResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
