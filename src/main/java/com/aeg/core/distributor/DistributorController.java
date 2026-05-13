package com.aeg.core.distributor;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.aeg.core.distributor.dto.DistributorRequest;
import com.aeg.core.distributor.dto.DistributorResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/distributors")
public class DistributorController {

	private final DistributorService service;

	public DistributorController(DistributorService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<DistributorResponse>> findAll() {
		return ResponseEntity.ok(service.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<DistributorResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	public ResponseEntity<DistributorResponse> create(@Valid @RequestBody DistributorRequest request) {
		DistributorResponse created = service.create(request);
		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();

		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<DistributorResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody DistributorRequest request
	) {
		return ResponseEntity.ok(service.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}