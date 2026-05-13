package com.aeg.core.printermodel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PrinterModelResponse(
        Long id,
        String brand,
        String modelCode,
        String providencia,
        LocalDate approvalDate,
        OffsetDateTime createdAt,
        BigDecimal price
) {}
