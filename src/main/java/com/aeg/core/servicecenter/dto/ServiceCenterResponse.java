package com.aeg.core.servicecenter.dto;

import java.time.LocalDateTime;

public record ServiceCenterResponse(

	Long id,
	Long branchId,
	LocalDateTime createdAt
) {
}