package com.aeg.core.modificationrequest.dto;

import com.aeg.core.employee.dto.EmployeeRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EmployeeModificationUpdateRequest(
		@NotNull Long employeeId,
		@NotNull @Valid EmployeeRequest proposedData) {
}
