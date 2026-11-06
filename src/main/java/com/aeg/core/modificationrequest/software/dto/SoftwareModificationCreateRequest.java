package com.aeg.core.modificationrequest.software.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SoftwareModificationCreateRequest(
        @NotNull Long printerId,
        @NotBlank String name,
        @NotBlank String version,
        List<String> programmingLanguages,
        List<String> operatingSystems,
        String developerName,
        String developerRif,
        Boolean external,
        List<Long> distributorCompanyIds
) {}
