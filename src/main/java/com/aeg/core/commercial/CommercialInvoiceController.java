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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.commercial.dto.CommercialInvoicePrintersRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;

@RestController
@RequestMapping("/api/commercial-invoices")
public class CommercialInvoiceController {

    private final CommercialInvoiceService service;

    public CommercialInvoiceController(CommercialInvoiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommercialInvoiceResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CommercialInvoiceResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialInvoiceResponse create(@Valid @RequestBody CommercialInvoiceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CommercialInvoiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CommercialInvoiceRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/printers")
    public CommercialInvoiceResponse syncPrinters(
            @PathVariable Long id,
            @RequestBody(required = false) CommercialInvoicePrintersRequest request) {
        List<Long> printerIds = request != null ? request.getPrinterIds() : List.of();
        return service.syncPrinters(id, printerIds);
    }

    @DeleteMapping("/{id}/printers/{printerId}")
    public CommercialInvoiceResponse unlinkPrinter(
            @PathVariable Long id,
            @PathVariable Long printerId) {
        return service.unlinkPrinter(id, printerId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
