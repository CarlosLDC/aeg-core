package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ToolsFormasPagoWriteRequest(
        @NotNull Long printerId,
        @NotNull Integer nroFP,
        @NotBlank String descripcion) {}
