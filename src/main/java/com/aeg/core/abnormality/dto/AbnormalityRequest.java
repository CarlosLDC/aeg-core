package com.aeg.core.abnormality.dto;

import java.time.OffsetDateTime;

import com.aeg.core.abnormality.AbnormalityType;

import jakarta.validation.constraints.NotNull;

public record AbnormalityRequest(
        @NotNull Long printerId,
        @NotNull AbnormalityType type,
        OffsetDateTime detectedAt,
        Boolean authorized,
        String description
) {}
