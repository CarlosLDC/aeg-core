package com.aeg.core.modificationrequest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EmployeeModificationUpdateRequest(
		@NotNull Long employeeId,
		@NotNull @Valid EmployeeModificationProposedData proposedData) {
}
