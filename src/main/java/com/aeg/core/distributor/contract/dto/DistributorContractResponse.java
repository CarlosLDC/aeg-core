package com.aeg.core.distributor.contract.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DistributorContractResponse(
		Long id,
		Long distributorId,
		LocalDate startDate,
		LocalDate endDate,
		OffsetDateTime createdAt,
		List<String> photoUrls) {
}
