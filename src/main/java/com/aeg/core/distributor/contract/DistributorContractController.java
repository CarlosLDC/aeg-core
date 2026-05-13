package com.aeg.core.distributor.contract;

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

import com.aeg.core.distributor.contract.dto.DistributorContractRequest;
import com.aeg.core.distributor.contract.dto.DistributorContractResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/distributor-contracts")
public class DistributorContractController {

	private final DistributorContractService service;

	public DistributorContractController(DistributorContractService service) {
		this.service = service;
	}

	@GetMapping
	public List<DistributorContractResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public DistributorContractResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DistributorContractResponse create(@Valid @RequestBody DistributorContractRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public DistributorContractResponse update(
			@PathVariable Long id,
			@Valid @RequestBody DistributorContractRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
