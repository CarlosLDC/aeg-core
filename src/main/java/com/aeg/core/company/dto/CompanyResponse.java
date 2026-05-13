package com.aeg.core.company.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
    Long id,
    String businessName,
    LocalDateTime createdAt,
    String rif,
    com.aeg.core.company.ContributorType contributorType
) {}
