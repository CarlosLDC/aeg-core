package com.aeg.core.printer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printer.PrinterTicketSection;

public record PrinterResponse(
        Long id,
        Long modelId,
        Long softwareId,
        Long clientId,
        String fiscalSerial,
        BigDecimal finalSalePrice,
        OffsetDateTime createdAt,
        UUID creationBatchId,
        PrinterStatus status,
        Long distributorId,
        Boolean paid,
        OffsetDateTime installationDate,
        String versionFirmware,
        String macAddress,
        DeviceType deviceType,
        PrinterTicketSection header,
        PrinterTicketSection trailer,
        String encryptionKey
) {
    public PrinterResponse(
            Long id,
            Long modelId,
            Long softwareId,
            Long clientId,
            String fiscalSerial,
            BigDecimal finalSalePrice,
            OffsetDateTime createdAt,
            UUID creationBatchId,
            PrinterStatus status,
            Long distributorId,
            Boolean paid,
            OffsetDateTime installationDate,
            String versionFirmware,
            String macAddress,
            DeviceType deviceType,
            PrinterTicketSection header,
            PrinterTicketSection trailer) {
        this(id, modelId, softwareId, clientId, fiscalSerial, finalSalePrice, createdAt,
                creationBatchId, status, distributorId, paid, installationDate,
                versionFirmware, macAddress, deviceType, header, trailer, null);
    }
}
