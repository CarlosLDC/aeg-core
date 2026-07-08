package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ToolsWifiConnectRequest(
        @NotNull Long printerId,
        @NotBlank String ssid,
        @NotBlank String password) {}
