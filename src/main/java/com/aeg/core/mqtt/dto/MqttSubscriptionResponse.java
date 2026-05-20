package com.aeg.core.mqtt.dto;

public record MqttSubscriptionResponse(String topic, boolean active) {
}
