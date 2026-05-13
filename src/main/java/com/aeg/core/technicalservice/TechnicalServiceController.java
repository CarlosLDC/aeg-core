package com.aeg.core.technicalservice;

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

import com.aeg.core.technicalservice.dto.TechnicalServiceRequest;
import com.aeg.core.technicalservice.dto.TechnicalServiceResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/technical-services")
public class TechnicalServiceController {

	private final TechnicalServiceService service;

	public TechnicalServiceController(TechnicalServiceService service) {
		this.service = service;
	}

	@GetMapping
	public List<TechnicalServiceResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public TechnicalServiceResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TechnicalServiceResponse create(@Valid @RequestBody TechnicalServiceRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public TechnicalServiceResponse update(
			@PathVariable Long id,
			@Valid @RequestBody TechnicalServiceRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
