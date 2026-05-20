package com.aeg.core.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;

public record BranchRequest(
        @NotNull Long companyId,
        @NotBlank String city,
        @NotBlank String state,
        String address,
        String phone,
        @Email String email,
        @NotBlank String contactPersonName,
        Boolean isClient,
        Boolean isDistributor,
        Boolean isServiceCenter
) {}
