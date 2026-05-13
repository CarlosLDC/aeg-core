package com.aeg.core.printermodel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrinterModelRequest(
        @NotBlank String brand,
        @NotBlank String modelCode,
        String providencia,
        LocalDate approvalDate,
        @NotNull @DecimalMin(value = "0.0000001") BigDecimal price
) {}
