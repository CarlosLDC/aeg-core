package com.aeg.core.servicecenter.contract;

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

import com.aeg.core.servicecenter.contract.dto.ServiceCenterContractRequest;
import com.aeg.core.servicecenter.contract.dto.ServiceCenterContractResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/service-center-contracts")
public class ServiceCenterContractController {

	private final ServiceCenterContractService service;

	public ServiceCenterContractController(ServiceCenterContractService service) {
		this.service = service;
	}

	@GetMapping
	public List<ServiceCenterContractResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public ServiceCenterContractResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServiceCenterContractResponse create(@Valid @RequestBody ServiceCenterContractRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public ServiceCenterContractResponse update(
			@PathVariable Long id,
			@Valid @RequestBody ServiceCenterContractRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
