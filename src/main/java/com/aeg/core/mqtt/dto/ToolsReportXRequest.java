package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record ToolsReportXRequest(
        @NotNull Long printerId,
        String mode) {}
