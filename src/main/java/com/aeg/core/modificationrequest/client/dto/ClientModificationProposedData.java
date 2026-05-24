package com.aeg.core.modificationrequest.client.dto;

import com.aeg.core.company.ContributorType;

public record ClientModificationProposedData(
		String businessName,
		String rif,
		ContributorType contributorType,
		String city,
		String state,
		String address,
		String contactPersonName,
		String phone,
		String email,
		Long distributorId) {
}
