package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record ToolsPrinterRequest(@NotNull Long printerId) {}
