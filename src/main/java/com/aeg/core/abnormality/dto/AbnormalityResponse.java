package com.aeg.core.abnormality.dto;

import java.time.OffsetDateTime;

import com.aeg.core.abnormality.AbnormalityOrigin;
import com.aeg.core.abnormality.AbnormalityType;

public record AbnormalityResponse(
        Long id,
        Long printerId,
        AbnormalityType type,
        OffsetDateTime detectedAt,
        boolean authorized,
        Long userId,
        String description,
        AbnormalityOrigin origin,
        Long originId,
        OffsetDateTime createdAt
) {}
