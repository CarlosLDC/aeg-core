package com.aeg.core.software.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SoftwareResponse(
        Long id,
        String name,
        String version,
        OffsetDateTime createdAt,
        List<String> programmingLanguages,
        List<String> operatingSystems
) {}
