package com.aeg.core.printer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.PrinterStatus;

public record PrinterRequest(
        @NotNull Long modelId,
        Long softwareId,
        Long clientId,
        Long distributorId,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}[0-9]{7}$", flags = Pattern.Flag.CASE_INSENSITIVE) String fiscalSerial,
        @DecimalMin(value = "0") BigDecimal finalSalePrice,
        @NotNull Boolean paid,
        OffsetDateTime installationDate,
        @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+$") String versionFirmware,
        @Pattern(regexp = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$") String macAddress,
        @NotNull PrinterStatus status,
        @NotNull DeviceType deviceType,
        UUID creationBatchId,
        String encryptionKey
) {
    public PrinterRequest(
            Long modelId,
            Long softwareId,
            Long clientId,
            Long distributorId,
            String fiscalSerial,
            BigDecimal finalSalePrice,
            Boolean paid,
            OffsetDateTime installationDate,
            String versionFirmware,
            String macAddress,
            PrinterStatus status,
            DeviceType deviceType,
            UUID creationBatchId) {
        this(modelId, softwareId, clientId, distributorId, fiscalSerial, finalSalePrice,
                paid, installationDate, versionFirmware, macAddress, status, deviceType,
                creationBatchId, null);
    }
}
