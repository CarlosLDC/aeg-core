package com.aeg.core.company.dto;

import java.time.LocalDateTime;

import com.aeg.core.company.OrganizationType;

public record CompanyResponse(
    Long id,
    String businessName,
    LocalDateTime createdAt,
    String rif,
    com.aeg.core.company.ContributorType contributorType,
    OrganizationType organizationType
) {}
