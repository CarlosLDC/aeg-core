package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record AnnualInspectionStaInfRequest(@NotNull Long printerId) {}
