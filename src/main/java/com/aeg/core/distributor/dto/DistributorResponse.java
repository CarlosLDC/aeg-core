package com.aeg.core.distributor.dto;

import java.time.LocalDateTime;

public record DistributorResponse(

	Long id,
	Long branchId,
	LocalDateTime createdAt
) {
}