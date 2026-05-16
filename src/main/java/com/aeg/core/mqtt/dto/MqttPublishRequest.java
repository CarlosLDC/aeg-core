package com.aeg.core.mqtt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MqttPublishRequest(
        @NotBlank String topic,
        @NotNull JsonNode payload
) {
}