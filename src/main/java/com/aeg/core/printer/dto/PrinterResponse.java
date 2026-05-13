package com.aeg.core.printer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.PrinterStatus;

public record PrinterResponse(
        Long id,
        Long modelId,
        Long softwareId,
        Long branchId,
        String fiscalSerial,
        BigDecimal finalSalePrice,
        OffsetDateTime createdAt,
        PrinterStatus status,
        Long distributorId,
        Boolean paid,
        OffsetDateTime installationDate,
        String versionFirmware,
        String macAddress,
        DeviceType deviceType
) {}
