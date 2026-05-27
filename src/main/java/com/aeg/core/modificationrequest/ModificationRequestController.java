package com.aeg.core.modificationrequest;

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

import com.aeg.core.modificationrequest.dto.EmployeeModificationDeleteRequest;
import com.aeg.core.modificationrequest.dto.EmployeeModificationUpdateRequest;
import com.aeg.core.modificationrequest.dto.ModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.dto.ModificationRequestListItemResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employee-modification-requests")
public class ModificationRequestController {

	private final ModificationRequestService service;

	public ModificationRequestController(ModificationRequestService service) {
		this.service = service;
	}

	@PostMapping("/update")
	@ResponseStatus(HttpStatus.CREATED)
	public ModificationRequestDetailResponse requestUpdate(
			@Valid @RequestBody EmployeeModificationUpdateRequest request) {
		return service.requestUpdate(request.employeeId(), request.proposedData());
	}

	@PostMapping("/delete")
	@ResponseStatus(HttpStatus.CREATED)
	public ModificationRequestDetailResponse requestDelete(
			@Valid @RequestBody EmployeeModificationDeleteRequest request) {
		return service.requestDelete(request.employeeId());
	}

	@GetMapping
	public List<ModificationRequestListItemResponse> findByStatus(
			@RequestParam(required = false) ModificationRequestStatus status) {
		return service.findByStatus(status);
	}

	@GetMapping("/{id}")
	public ModificationRequestDetailResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping("/{id}/approve")
	public ModificationRequestDetailResponse approve(@PathVariable Long id) {
		return service.approve(id);
	}

	@PostMapping("/{id}/reject")
	public ModificationRequestDetailResponse reject(@PathVariable Long id) {
		return service.reject(id);
	}

	@PostMapping("/{id}/cancel")
	public ModificationRequestDetailResponse cancel(@PathVariable Long id) {
		return service.cancel(id);
	}
}
