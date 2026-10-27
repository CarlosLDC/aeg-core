package com.aeg.core.decommission.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.aeg.core.decommission.DecommissionReason;

public record DecommissionResponse(
        Long id,
        Long printerId,
        LocalDate date,
        DecommissionReason reason,
        boolean ownPrinter,
        Long userId,
        String notes,
        OffsetDateTime createdAt
) {}
