package com.aeg.core.modificationrequest.software.dto;

import java.time.OffsetDateTime;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;

public record SoftwareModificationRequestListItemResponse(
        Long id,
        Long printerId,
        String printerFiscalSerial,
        String softwareName,
        String softwareVersion,
        ModificationActionType actionType,
        ModificationRequestStatus status,
        Long requestedById,
        String requestedByName,
        OffsetDateTime createdAt
) {}
