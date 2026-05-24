package com.aeg.core.modificationrequest.dto;

import java.time.OffsetDateTime;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;

public record ModificationRequestListItemResponse(
		Long id,
		Long employeeId,
		String employeeName,
		ModificationActionType actionType,
		ModificationRequestStatus status,
		Long requestedById,
		String requestedByName,
		OffsetDateTime createdAt) {
}
