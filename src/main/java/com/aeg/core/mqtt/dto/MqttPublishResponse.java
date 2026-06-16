package com.aeg.core.mqtt.dto;

public record MqttPublishResponse(
        String status,
        String topic,
        Object payload,
        String broker,
        MqttPublishEnajenacionResult enajenacion) {

    public MqttPublishResponse(String status, String topic, Object payload, String broker) {
        this(status, topic, payload, broker, null);
    }
}