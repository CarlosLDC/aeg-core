package com.aeg.core.mqtt;

public record MqttConnectionProbeResult(
        boolean success,
        boolean connected,
        String broker,
        long durationMs,
        String message
) {
}
