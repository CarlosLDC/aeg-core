package com.aeg.core.modificationrequest.printer.dto;

import java.time.OffsetDateTime;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.printer.PrinterStatus;

public record PrinterEnajenacionRequestListItemResponse(
        Long id,
        Long printerId,
        String fiscalSerial,
        String distributorName,
        PrinterStatus printerStatus,
        Integer suggestedInstallments,
        String notes,
        ModificationActionType actionType,
        ModificationRequestStatus status,
        Long requestedById,
        String requestedByName,
        OffsetDateTime createdAt
) {
}
