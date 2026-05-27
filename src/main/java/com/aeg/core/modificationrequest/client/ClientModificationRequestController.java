package com.aeg.core.modificationrequest.client;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.client.dto.ClientModificationDeleteRequest;
import com.aeg.core.modificationrequest.client.dto.ClientModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.client.dto.ClientModificationRequestListItemResponse;
import com.aeg.core.modificationrequest.client.dto.ClientModificationUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/client-modification-requests")
public class ClientModificationRequestController {

	private final ClientModificationRequestService service;

	public ClientModificationRequestController(ClientModificationRequestService service) {
		this.service = service;
	}

	@PostMapping("/update")
	@ResponseStatus(HttpStatus.CREATED)
	public ClientModificationRequestDetailResponse requestUpdate(
			@Valid @RequestBody ClientModificationUpdateRequest request) {
		return service.requestUpdate(request.clientId(), request.proposedData());
	}

	@PostMapping("/delete")
	@ResponseStatus(HttpStatus.CREATED)
	public ClientModificationRequestDetailResponse requestDelete(
			@Valid @RequestBody ClientModificationDeleteRequest request) {
		return service.requestDelete(request.clientId());
	}

	@GetMapping
	public List<ClientModificationRequestListItemResponse> findByStatus(
			@RequestParam(required = false) ModificationRequestStatus status) {
		return service.findByStatus(status);
	}

	@GetMapping("/{id}")
	public ClientModificationRequestDetailResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping("/{id}/approve")
	public ClientModificationRequestDetailResponse approve(@PathVariable Long id) {
		return service.approve(id);
	}

	@PostMapping("/{id}/reject")
	public ClientModificationRequestDetailResponse reject(@PathVariable Long id) {
		return service.reject(id);
	}

	@PostMapping("/{id}/cancel")
	public ClientModificationRequestDetailResponse cancel(@PathVariable Long id) {
		return service.cancel(id);
	}
}
