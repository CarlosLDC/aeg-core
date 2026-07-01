package com.aeg.core.client.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClientTransferDistributorRequest(

	@NotNull(message = "distributorId is required")
	@Positive(message = "distributorId must be a positive number")
	Long distributorId
) {
}
