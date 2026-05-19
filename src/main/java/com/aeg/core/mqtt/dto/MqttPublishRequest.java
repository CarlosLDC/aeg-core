package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code payload} acepta objeto o array JSON; Jackson lo materializa como
 * {@link java.util.Map} o {@link java.util.List}.
 */
public record MqttPublishRequest(
        @NotBlank String topic,
        @NotNull Object payload
) {
}
