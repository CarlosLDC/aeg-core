package com.aeg.core.modificationrequest.software.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.software.dto.SoftwareDistributorResponse;

public record SoftwareModificationRequestDetailResponse(
        Long id,
        Long printerId,
        String printerFiscalSerial,
        String printerModelCode,
        String distributorName,
        ModificationActionType actionType,
        ModificationRequestStatus status,
        String name,
        String version,
        List<String> programmingLanguages,
        List<String> operatingSystems,
        String developerName,
        String developerRif,
        boolean external,
        List<SoftwareDistributorResponse> distributors,
        Long requestedById,
        String requestedByName,
        OffsetDateTime createdAt
) {}
