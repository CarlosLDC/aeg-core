package com.aeg.core.distributor.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DistributorRequest(

	@NotNull(message = "branchId is required")
	@Positive(message = "branchId must be a positive number")
	Long branchId,

	/** Null = keep existing on update / default true on create. */
	Boolean canWriteAnnualInspection,

	LocalDate inclusionDate,
	LocalDate exclusionDate
) {
	public DistributorRequest(Long branchId) {
		this(branchId, null, null, null);
	}

	public DistributorRequest(Long branchId, Boolean canWriteAnnualInspection) {
		this(branchId, canWriteAnnualInspection, null, null);
	}
}
