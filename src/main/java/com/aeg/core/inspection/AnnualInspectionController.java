package com.aeg.core.inspection;

import java.util.List;

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

import com.aeg.core.inspection.dto.AnnualInspectionRequest;
import com.aeg.core.inspection.dto.AnnualInspectionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/annual-inspections")
public class AnnualInspectionController {

	private final AnnualInspectionService service;

	public AnnualInspectionController(AnnualInspectionService service) {
		this.service = service;
	}

	@GetMapping
	public List<AnnualInspectionResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public AnnualInspectionResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AnnualInspectionResponse create(@Valid @RequestBody AnnualInspectionRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public AnnualInspectionResponse update(
			@PathVariable Long id,
			@Valid @RequestBody AnnualInspectionRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
