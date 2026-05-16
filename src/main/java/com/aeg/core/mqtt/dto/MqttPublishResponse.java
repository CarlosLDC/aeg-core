package com.aeg.core.mqtt.dto;

public record MqttPublishResponse(
        String status,
        String topic,
        String payload,
        String broker
) {
}