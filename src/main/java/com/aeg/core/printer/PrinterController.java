package com.aeg.core.printer;

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

import com.aeg.core.printer.dto.PrinterDispositionRequest;
import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printer.dto.PrinterResponse;

@RestController
@RequestMapping("/api/printers")
public class PrinterController {

    private final PrinterService service;

    public PrinterController(PrinterService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrinterResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PrinterResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrinterResponse create(@Valid @RequestBody PrinterRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PrinterResponse update(@PathVariable Long id, @Valid @RequestBody PrinterRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/enajenar")
    public PrinterResponse dispose(@PathVariable Long id, @Valid @RequestBody PrinterDispositionRequest request) {
        return service.dispose(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
