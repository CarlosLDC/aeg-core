package com.aeg.core.seal.dto;

import java.time.OffsetDateTime;

import com.aeg.core.seal.SealColor;
import com.aeg.core.seal.SealStatus;

public record SealResponse(
        Long id,
        Long printerId,
        String serial,
        OffsetDateTime createdAt,
        OffsetDateTime installationDate,
        OffsetDateTime removalDate,
        SealColor color,
        SealStatus status
) {}
