package com.aeg.core.decommission.dto;

import java.time.LocalDate;

import com.aeg.core.decommission.DecommissionReason;

import jakarta.validation.constraints.NotNull;

public record DecommissionRequest(
        @NotNull DecommissionReason reason,
        LocalDate date,
        Boolean ownPrinter,
        String notes
) {}
