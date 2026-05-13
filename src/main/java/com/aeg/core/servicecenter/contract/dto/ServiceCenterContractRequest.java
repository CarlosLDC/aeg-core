package com.aeg.core.servicecenter.contract.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ServiceCenterContractRequest(
		@NotNull Long serviceCenterId,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@NotNull List<String> photoUrls) {
}
