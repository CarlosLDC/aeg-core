package com.aeg.core.technicalservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TechnicalServiceRequest(
		@NotNull Long printerId,
		@NotNull Long userId,
		Long serviceCenterId,
		@NotNull Boolean sealTampered,
		String notes,
		@NotNull OffsetDateTime startAt,
		@NotNull OffsetDateTime endAt,
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
