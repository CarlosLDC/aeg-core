package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MqttPublishRequest(
        @NotBlank String topic,
        @NotNull String payload
) {
}