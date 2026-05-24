package com.aeg.core.modificationrequest.dto;

import com.aeg.core.employee.EmployeeType;
import com.aeg.core.employee.dto.EmployeeRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeModificationProposedData(
		@NotBlank String nationalId,
		@NotBlank String name,
		@NotBlank String phone,
		@NotBlank String email,
		@NotNull EmployeeType type,
		@NotNull Long branchId,
		Boolean isTechnician,
		Boolean isDistributorPerson) {

	public EmployeeRequest toEmployeeRequest() {
		return new EmployeeRequest(nationalId, name, phone, email, type, branchId);
	}
}
