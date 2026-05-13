package com.aeg.core.inspeccion.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record InspeccionAnualResponse(
		Long id,
		Long printerId,
		Long employeeId,
		Boolean sealTampered,
		String notes,
		OffsetDateTime createdAt,
		List<String> photoUrls,
		LocalDate inspectionDate) {
}
