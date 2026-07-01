package com.aeg.core.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.client.ClientService;
import com.aeg.core.client.dto.ClientResponse;
import com.aeg.core.client.dto.ClientTransferDistributorRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/clients")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClientController {

	private final ClientService clientService;

	public AdminClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@PostMapping("/{id}/transfer-distributor")
	public ResponseEntity<ClientResponse> transferDistributor(
			@PathVariable Long id,
			@Valid @RequestBody ClientTransferDistributorRequest request) {
		return ResponseEntity.ok(clientService.transferDistributor(id, request.distributorId()));
	}
}
