package com.aeg.core.modificationrequest.printer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrinterEnajenacionCreateRequest(
        @NotNull(message = "printerId is required")
        Long printerId,
        @Min(value = 1, message = "suggestedInstallments must be at least 1")
        Integer suggestedInstallments,
        String notes
) {
}
