package com.aeg.core.employee.dto;

import com.aeg.core.employee.EmployeeType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
		@NotBlank String nationalId,
		@NotBlank String name,
		@NotBlank String phone,
		@NotBlank String email,
		@NotNull EmployeeType type,
		@NotNull Long branchId) {
}
