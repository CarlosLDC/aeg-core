package com.aeg.core.inspection.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AnnualInspectionRequest(
		@NotNull Long printerId,
		@NotNull Long employeeId,
		@NotNull Boolean sealTampered,
		String notes,
		@NotNull List<String> photoUrls,
		LocalDate inspectionDate) {
}
