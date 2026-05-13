package com.aeg.core.seal;

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

import com.aeg.core.seal.dto.SealRequest;
import com.aeg.core.seal.dto.SealResponse;

@RestController
@RequestMapping("/api/seals")
public class SealController {

    private final SealService service;

    public SealController(SealService service) {
        this.service = service;
    }

    @GetMapping
    public List<SealResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SealResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SealResponse create(@Valid @RequestBody SealRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SealResponse update(@PathVariable Long id, @Valid @RequestBody SealRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
