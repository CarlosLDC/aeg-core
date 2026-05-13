package com.aeg.core.technicalservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TechnicalServiceRequest(
		@NotNull Long printerId,
		@NotNull Long technicianId,
		Long serviceCenterId,
		@NotNull Boolean sealTampered,
		String notes,
		@NotNull OffsetDateTime startAt,
		@NotNull OffsetDateTime endAt,
		@NotNull List<String> photoUrls,
		Long installedSealId,
		Long removedSealId,
		@NotNull Integer initialZReport,
		@NotNull Integer finalZReport,
		@NotNull BigDecimal cost,
		@NotBlank String reportedFailure,
		@NotNull LocalDate requestDate,
		@NotNull OffsetDateTime initialZDate,
		@NotNull OffsetDateTime finalZDate,
		Long distributorId) {
}
