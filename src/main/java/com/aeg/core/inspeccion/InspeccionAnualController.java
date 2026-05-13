package com.aeg.core.inspeccion;

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

import com.aeg.core.inspeccion.dto.InspeccionAnualRequest;
import com.aeg.core.inspeccion.dto.InspeccionAnualResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/annual-inspections")
public class InspeccionAnualController {

	private final InspeccionAnualService service;

	public InspeccionAnualController(InspeccionAnualService service) {
		this.service = service;
	}

	@GetMapping
	public List<InspeccionAnualResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public InspeccionAnualResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InspeccionAnualResponse create(@Valid @RequestBody InspeccionAnualRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public InspeccionAnualResponse update(
			@PathVariable Long id,
			@Valid @RequestBody InspeccionAnualRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
