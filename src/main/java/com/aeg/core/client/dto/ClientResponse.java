package com.aeg.core.client.dto;

import java.time.LocalDateTime;

public record ClientResponse(
		Long id,
		Long branchId,
		Long distributorId,
		LocalDateTime createdAt,
		String branchCity,
		String branchState,
		String companyBusinessName,
		String companyRif,
		String branchPhone,
		String branchEmail) {

	public ClientResponse(Long id, Long branchId, Long distributorId, LocalDateTime createdAt) {
		this(id, branchId, distributorId, createdAt, null, null, null, null, null, null);
	}
}