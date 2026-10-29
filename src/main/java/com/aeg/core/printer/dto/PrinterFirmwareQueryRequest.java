package com.aeg.core.printer.dto;

import jakarta.validation.constraints.NotBlank;

public record PrinterFirmwareQueryRequest(
        @NotBlank String macAddress,
        Long modelId
) {}
