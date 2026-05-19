package com.aeg.core.branch;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.branch.dto.BranchRequest;
import com.aeg.core.branch.dto.BranchResponse;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @GetMapping
    public List<BranchResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/lookup")
    public ResponseEntity<BranchResponse> lookupByLocation(
            @RequestParam("companyId") Long companyId,
            @RequestParam("city") String city,
            @RequestParam("state") String state) {
        return service
                .lookupByCompanyLocation(companyId, city, state)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public BranchResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(@Valid @RequestBody BranchRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public BranchResponse update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
