package com.aeg.core.software;

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

import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;

@RestController
@RequestMapping("/api/software")
public class SoftwareController {

    private final SoftwareService service;

    public SoftwareController(SoftwareService service) {
        this.service = service;
    }

    @GetMapping
    public List<SoftwareResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SoftwareResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SoftwareResponse create(@Valid @RequestBody SoftwareRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SoftwareResponse update(@PathVariable Long id, @Valid @RequestBody SoftwareRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
