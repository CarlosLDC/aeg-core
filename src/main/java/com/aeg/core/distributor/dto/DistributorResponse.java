package com.aeg.core.distributor.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DistributorResponse(

	Long id,
	Long branchId,
	LocalDateTime createdAt,
	Boolean canWriteAnnualInspection,
	LocalDate inclusionDate,
	LocalDate exclusionDate
) {
	public DistributorResponse(Long id, Long branchId, LocalDateTime createdAt, Boolean canWriteAnnualInspection) {
		this(id, branchId, createdAt, canWriteAnnualInspection, null, null);
	}
}
