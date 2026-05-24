package com.aeg.core.client.dto;

import java.time.LocalDateTime;

import com.aeg.core.client.ClientReviewStatus;

public record ClientResponse(
		Long id,
		Long branchId,
		Long distributorId,
		LocalDateTime createdAt,
		ClientReviewStatus reviewStatus,
		Long activeModificationRequestId,
		String branchCity,
		String branchState,
		String companyBusinessName,
		String companyRif,
		String branchPhone,
		String branchEmail) {

	public ClientResponse(Long id, Long branchId, Long distributorId, LocalDateTime createdAt) {
		this(id, branchId, distributorId, createdAt, ClientReviewStatus.ACTIVE, null, null, null, null, null, null, null);
	}
}