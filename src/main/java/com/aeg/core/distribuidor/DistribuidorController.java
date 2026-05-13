package com.aeg.core.distribuidor;

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

import com.aeg.core.distribuidor.dto.DistribuidorRequest;
import com.aeg.core.distribuidor.dto.DistribuidorResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/distribuidores")
public class DistribuidorController {

	private final DistribuidorService service;

	public DistribuidorController(DistribuidorService service) {
		this.service = service;
	}

	@GetMapping
	public List<DistribuidorResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public DistribuidorResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DistribuidorResponse create(@Valid @RequestBody DistribuidorRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public DistribuidorResponse update(@PathVariable Long id, @Valid @RequestBody DistribuidorRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
