package com.aeg.core.printermodel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PrinterModelRequest(
        @NotBlank String modelCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", flags = Pattern.Flag.CASE_INSENSITIVE) String serialPrefix,
        String providencia,
        LocalDate approvalDate,
        @NotNull @DecimalMin(value = "0.0000001") BigDecimal price
) {}
