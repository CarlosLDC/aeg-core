package com.aeg.core.printer.dto;

import java.time.OffsetDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.aeg.core.printer.PrinterTicketSection;

public record PrinterDispositionRequest(
        @NotNull Long clientId,
        OffsetDateTime installationDate,
        @NotNull @Valid PrinterTicketSection header,
        @NotNull @Valid PrinterTicketSection trailer
) {}
