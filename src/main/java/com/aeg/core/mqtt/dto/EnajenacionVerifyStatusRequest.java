package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record EnajenacionVerifyStatusRequest(
        @NotNull(message = "printerId is required") Long printerId,
        boolean autoSync) {}
