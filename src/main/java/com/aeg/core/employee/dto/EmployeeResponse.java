package com.aeg.core.employee.dto;

import java.time.OffsetDateTime;

import com.aeg.core.employee.EmployeeType;

public record EmployeeResponse(
		Long id,
		String nationalId,
		String name,
		String phone,
		String email,
		OffsetDateTime createdAt,
		EmployeeType type,
		Long branchId) {
}
