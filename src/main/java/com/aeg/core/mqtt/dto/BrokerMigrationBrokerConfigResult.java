package com.aeg.core.mqtt.dto;

public record BrokerMigrationBrokerConfigResult(
    Outcome outcome,
    String message
) {
    public enum Outcome {
        MIGRATED,
        REJECTED,
        UNCERTAIN
    }
}
