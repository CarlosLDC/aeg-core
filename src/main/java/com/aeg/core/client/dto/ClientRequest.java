package com.aeg.core.client.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClientRequest(

	@NotNull(message = "branchId is required")
	@Positive(message = "branchId must be a positive number")
	Long branchId,

	@Positive(message = "distributorId must be a positive number")
	Long distributorId
) {
}