package com.aeg.core.modificationrequest.dto;

import java.time.OffsetDateTime;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.fasterxml.jackson.databind.JsonNode;

public record ModificationRequestDetailResponse(
		Long id,
		Long employeeId,
		ModificationActionType actionType,
		ModificationRequestStatus status,
		JsonNode proposedData,
		EmployeeSnapshotResponse currentEmployeeSnapshot,
		Long requestedById,
		String requestedByName,
		OffsetDateTime createdAt) {
}
