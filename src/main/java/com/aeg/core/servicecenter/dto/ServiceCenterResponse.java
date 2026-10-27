package com.aeg.core.servicecenter.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceCenterResponse(

	Long id,
	Long branchId,
	LocalDateTime createdAt,
	LocalDate inclusionDate,
	LocalDate exclusionDate
) {
	public ServiceCenterResponse(Long id, Long branchId, LocalDateTime createdAt) {
		this(id, branchId, createdAt, null, null);
	}
}
