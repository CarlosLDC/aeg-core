package com.aeg.core.modificationrequest.printer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.printer.PrinterStatus;

public record PrinterEnajenacionRequestDetailResponse(
        Long id,
        Long printerId,
        String fiscalSerial,
        String modelCode,
        Long distributorId,
        String distributorName,
        PrinterStatus printerStatus,
        BigDecimal finalSalePrice,
        Integer currentCreditInstallments,
        Boolean enajenacionAutorizada,
        Integer suggestedInstallments,
        String notes,
        Integer approvedInstallments,
        String reviewNote,
        ModificationActionType actionType,
        ModificationRequestStatus status,
        Long requestedById,
        String requestedByName,
        OffsetDateTime createdAt
) {
}
