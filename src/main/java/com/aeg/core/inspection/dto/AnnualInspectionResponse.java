package com.aeg.core.inspection.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AnnualInspectionResponse(
		Long id,
		Long printerId,
		Long userId,
		Boolean sealTampered,
		String notes,
		OffsetDateTime createdAt,
		List<String> photoUrls,
		LocalDate inspectionDate,
		String mqttRegistroImpresora,
		Long mqttSetDateRevOAt,
		Integer mqttNumeroFacturaPrueba) {
}
