package com.aeg.core.servicecenter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServiceCenterRequest(

	@NotNull(message = "branchId is required")
	@Positive(message = "branchId must be a positive number")
	Long branchId
) {
}