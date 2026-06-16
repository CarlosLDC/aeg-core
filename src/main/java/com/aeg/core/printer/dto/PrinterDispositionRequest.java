package com.aeg.core.printer.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record PrinterDispositionRequest(
        @NotNull Long clientId,
        OffsetDateTime installationDate
) {}
