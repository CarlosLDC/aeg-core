package com.aeg.core.seal.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.aeg.core.seal.SealColor;
import com.aeg.core.seal.SealStatus;

public record SealRequest(
        Long printerId,
        @NotBlank String serial,
        OffsetDateTime installationDate,
        OffsetDateTime removalDate,
        @NotNull SealColor color,
        @NotNull SealStatus status
) {}
