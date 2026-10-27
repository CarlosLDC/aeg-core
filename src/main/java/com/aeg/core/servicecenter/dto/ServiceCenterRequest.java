package com.aeg.core.servicecenter.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServiceCenterRequest(

	@NotNull(message = "branchId is required")
	@Positive(message = "branchId must be a positive number")
	Long branchId,

	LocalDate inclusionDate,
	LocalDate exclusionDate
) {
	public ServiceCenterRequest(Long branchId) {
		this(branchId, null, null);
	}
}
