package com.aeg.core.servicecenter;

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

import com.aeg.core.servicecenter.dto.ServiceCenterRequest;
import com.aeg.core.servicecenter.dto.ServiceCenterResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/service-centers")
public class ServiceCenterController {

	private final ServiceCenterService service;

	public ServiceCenterController(ServiceCenterService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<ServiceCenterResponse>> findAll() {
		return ResponseEntity.ok(service.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ServiceCenterResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	public ResponseEntity<ServiceCenterResponse> create(@Valid @RequestBody ServiceCenterRequest request) {
		ServiceCenterResponse created = service.create(request);
		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();

		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ServiceCenterResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody ServiceCenterRequest request
	) {
		return ResponseEntity.ok(service.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}