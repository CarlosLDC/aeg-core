package com.aeg.core.printermodel;

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

import com.aeg.core.printermodel.dto.PrinterModelRequest;
import com.aeg.core.printermodel.dto.PrinterModelResponse;

@RestController
@RequestMapping("/api/printer-models")
public class PrinterModelController {

    private final PrinterModelService service;

    public PrinterModelController(PrinterModelService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrinterModelResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PrinterModelResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrinterModelResponse create(@Valid @RequestBody PrinterModelRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PrinterModelResponse update(@PathVariable Long id, @Valid @RequestBody PrinterModelRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
