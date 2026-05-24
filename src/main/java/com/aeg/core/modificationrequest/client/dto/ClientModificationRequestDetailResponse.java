package com.aeg.core.modificationrequest.client.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;

public record ClientModificationRequestDetailResponse(
		Long id,
		Long clientId,
		ModificationActionType actionType,
		ModificationRequestStatus status,
		Map<String, Object> proposedData,
		ClientSnapshotResponse currentClientSnapshot,
		Long requestedById,
		String requestedByName,
		OffsetDateTime createdAt) {
}
