package com.aeg.core.modificationrequest.client.dto;

import java.time.OffsetDateTime;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;

public record ClientModificationRequestListItemResponse(
		Long id,
		Long clientId,
		String clientName,
		ModificationActionType actionType,
		ModificationRequestStatus status,
		Long requestedById,
		String requestedByName,
		OffsetDateTime createdAt) {
}
