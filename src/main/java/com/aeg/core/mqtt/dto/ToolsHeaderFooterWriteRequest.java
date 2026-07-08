package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record ToolsHeaderFooterWriteRequest(@NotNull Long printerId, @NotNull String content) {}
