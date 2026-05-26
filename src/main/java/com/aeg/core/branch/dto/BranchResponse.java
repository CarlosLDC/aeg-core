package com.aeg.core.branch.dto;

import java.time.OffsetDateTime;

public record BranchResponse(
        Long id,
        Long companyId,
        String city,
        String state,
        String address,
        String phone,
        String email,
        String contactPersonName,
        OffsetDateTime createdAt,
        Boolean isClient,
        Boolean isDistributor,
        Boolean isServiceCenter
) {}
