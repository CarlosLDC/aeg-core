package com.aeg.core.distributor.contract.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record DistributorContractRequest(
		@NotNull Long distributorId,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@NotNull List<String> photoUrls) {
}
