package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record ToolsReprintRequest(
        @NotNull Long printerId,
        String docType,
        Integer number,
        String mode) {}
