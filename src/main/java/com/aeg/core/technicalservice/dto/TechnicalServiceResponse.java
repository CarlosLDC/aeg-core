package com.aeg.core.technicalservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TechnicalServiceResponse(
		Long id,
		Long printerId,
		Long userId,
		Long serviceCenterId,
		Boolean sealTampered,
		String notes,
		OffsetDateTime startAt,
		OffsetDateTime createdAt,
		OffsetDateTime endAt,
		List<String> photoUrls,
		Long installedSealId,
		Long removedSealId,
		Integer initialZReport,
		Integer finalZReport,
		BigDecimal cost,
		String reportedFailure,
		LocalDate requestDate,
		OffsetDateTime initialZDate,
		OffsetDateTime finalZDate,
		Long distributorId) {
}
