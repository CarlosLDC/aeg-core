package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record ToolsReportZGetRequest(@NotNull Long printerId, @NotNull Integer reportNumber) {}
