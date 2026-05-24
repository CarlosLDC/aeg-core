package com.aeg.core.modificationrequest.dto;

import com.aeg.core.employee.EmployeeReviewStatus;
import com.aeg.core.employee.EmployeeType;

public record EmployeeSnapshotResponse(
		Long id,
		String nationalId,
		String name,
		String phone,
		String email,
		EmployeeType type,
		Long branchId,
		EmployeeReviewStatus reviewStatus) {
}
