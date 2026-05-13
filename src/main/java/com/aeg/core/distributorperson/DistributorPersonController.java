package com.aeg.core.distributorperson;

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

import com.aeg.core.distributorperson.dto.DistributorPersonRequest;
import com.aeg.core.distributorperson.dto.DistributorPersonResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/distributor-persons")
public class DistributorPersonController {

	private final DistributorPersonService service;

	public DistributorPersonController(DistributorPersonService service) {
		this.service = service;
	}

	@GetMapping
	public List<DistributorPersonResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public DistributorPersonResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DistributorPersonResponse create(@Valid @RequestBody DistributorPersonRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public DistributorPersonResponse update(
			@PathVariable Long id,
			@Valid @RequestBody DistributorPersonRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
