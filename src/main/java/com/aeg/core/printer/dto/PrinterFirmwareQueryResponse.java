package com.aeg.core.printer.dto;

public record PrinterFirmwareQueryResponse(
        String versionFirmware,
        Long firmwareId
) {}
