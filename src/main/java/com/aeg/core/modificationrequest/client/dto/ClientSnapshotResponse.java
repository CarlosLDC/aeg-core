package com.aeg.core.modificationrequest.client.dto;

import com.aeg.core.client.ClientReviewStatus;
import com.aeg.core.company.ContributorType;

public record ClientSnapshotResponse(
		Long id,
		Long branchId,
		Long distributorId,
		ClientReviewStatus reviewStatus,
		Long companyId,
		String businessName,
		String rif,
		ContributorType contributorType,
		String city,
		String state,
		String address,
		String contactPersonName,
		String phone,
		String email) {
}
