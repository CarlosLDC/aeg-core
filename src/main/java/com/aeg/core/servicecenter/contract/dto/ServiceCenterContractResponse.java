package com.aeg.core.servicecenter.contract.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ServiceCenterContractResponse(
		Long id,
		Long serviceCenterId,
		LocalDate startDate,
		LocalDate endDate,
		OffsetDateTime createdAt,
		List<String> photoUrls) {
}
