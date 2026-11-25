package com.aeg.core.company.dto;

import com.aeg.core.enajenacion.mqtt.RifFormatter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompanyRequest(
    String businessName,

    @NotNull(message = "rif is required")
    @Pattern(regexp = RifFormatter.FISCAL_RIF_PATTERN, message = RifFormatter.FISCAL_RIF_ERROR_MESSAGE)
    String rif,

    @NotNull(message = "contributorType is required")
    com.aeg.core.company.ContributorType contributorType
) {}
