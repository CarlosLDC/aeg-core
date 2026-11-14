package com.aeg.core.commercial;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.commercial.dto.CommercialPaymentRequest;
import com.aeg.core.commercial.dto.CommercialPaymentResponse;

@RestController
@RequestMapping("/api/commercial-payments")
public class CommercialPaymentController {

    private final CommercialPaymentService service;

    public CommercialPaymentController(CommercialPaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommercialPaymentResponse> findAll(
            @RequestParam(required = false) Long facturaComercialId) {
        return service.findAll(facturaComercialId);
    }

    @GetMapping("/{id}")
    public CommercialPaymentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialPaymentResponse create(@Valid @RequestBody CommercialPaymentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CommercialPaymentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CommercialPaymentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
