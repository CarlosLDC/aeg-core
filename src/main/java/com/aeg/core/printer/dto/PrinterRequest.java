package com.aeg.core.printer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.aeg.core.printer.PrinterStatus;

public record PrinterRequest(
        @NotNull Long modelId,
        Long softwareId,
        Long clientId,
        Long distributorId,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}[0-9]{7}$", flags = Pattern.Flag.CASE_INSENSITIVE) String fiscalSerial,
        @DecimalMin(value = "0") BigDecimal finalSalePrice,
        Boolean paid,
        OffsetDateTime installationDate,
        Long firmwareId,
        @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+$") String versionFirmware,
        @Pattern(regexp = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$") String macAddress,
        @NotNull PrinterStatus status,
        UUID creationBatchId,
        String encryptionKey,
        Long boxId
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
            Long firmwareId,
            String versionFirmware,
            String macAddress,
            PrinterStatus status,
            UUID creationBatchId,
            String encryptionKey) {
        this(modelId, softwareId, clientId, distributorId, fiscalSerial, finalSalePrice,
                paid, installationDate, firmwareId, versionFirmware, macAddress, status,
                creationBatchId, encryptionKey, null);
    }

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
            UUID creationBatchId,
            String encryptionKey) {
        this(modelId, softwareId, clientId, distributorId, fiscalSerial, finalSalePrice,
                paid, installationDate, null, versionFirmware, macAddress, status,
                creationBatchId, encryptionKey);
    }

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
            UUID creationBatchId) {
        this(modelId, softwareId, clientId, distributorId, fiscalSerial, finalSalePrice,
                paid, installationDate, null, versionFirmware, macAddress, status,
                creationBatchId, null);
    }
}
