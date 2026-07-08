package com.aeg.core.seal.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.aeg.core.seal.SealColor;
import com.aeg.core.seal.SealStatus;

public record SealResponse(
        Long id,
        Long printerId,
        String serial,
        OffsetDateTime createdAt,
        UUID creationBatchId,
        OffsetDateTime installationDate,
        OffsetDateTime removalDate,
        SealColor color,
        SealStatus status
) {}
