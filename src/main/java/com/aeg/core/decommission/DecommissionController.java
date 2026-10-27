package com.aeg.core.decommission;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.decommission.dto.DecommissionRequest;
import com.aeg.core.decommission.dto.DecommissionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/printers/{printerId}/desincorporaciones")
public class DecommissionController {

    private final DecommissionService service;

    public DecommissionController(DecommissionService service) {
        this.service = service;
    }

    @GetMapping
    public List<DecommissionResponse> findByPrinter(@PathVariable Long printerId) {
        return service.findByPrinter(printerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DecommissionResponse decommission(
            @PathVariable Long printerId, @Valid @RequestBody DecommissionRequest request) {
        return service.decommission(printerId, request);
    }
}
