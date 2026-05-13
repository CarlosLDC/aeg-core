package com.aeg.core.distributor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DistributorRequest(

	@NotNull(message = "branchId is required")
	@Positive(message = "branchId must be a positive number")
	Long branchId
) {
}