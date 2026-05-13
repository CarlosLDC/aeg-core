package com.aeg.core.company.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompanyRequest(
    String businessName,

    @NotNull(message = "rif is required")
    @Pattern(regexp = "^[VEJPG][0-9]{7,9}$", message = "rif must match pattern ^[VEJPG][0-9]{7,9}$")
    String rif,

    @NotNull(message = "contributorType is required")
    com.aeg.core.company.ContributorType contributorType
) {}
