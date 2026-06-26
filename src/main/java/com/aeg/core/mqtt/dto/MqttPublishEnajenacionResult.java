package com.aeg.core.mqtt.dto;

import com.aeg.core.enajenacion.mqtt.EnajenacionStartStatus;

public record MqttPublishEnajenacionResult(
        EnajenacionStartStatus status,
        String message) {
}
