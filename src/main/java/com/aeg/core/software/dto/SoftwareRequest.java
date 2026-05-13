package com.aeg.core.software.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SoftwareRequest(
        @NotBlank String name,
        @NotBlank String version,
        List<String> programmingLanguages,
        List<String> operatingSystems
) {}
