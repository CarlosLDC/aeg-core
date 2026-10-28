package com.aeg.core.software.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record SoftwareRequest(
        @NotBlank String name,
        @NotBlank String version,
        List<String> programmingLanguages,
        List<String> operatingSystems,
        String developerName,
        String developerRif,
        Boolean external,
        List<Long> distributorCompanyIds
) {}
