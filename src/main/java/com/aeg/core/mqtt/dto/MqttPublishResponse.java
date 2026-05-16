package com.aeg.core.mqtt.dto;

public record MqttPublishResponse(
        String status,
        String topic,
        Object payload,
        String broker
) {
}