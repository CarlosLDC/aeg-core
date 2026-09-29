package com.aeg.core.mqtt.dto;

public record BrokerMigrationBrokerResult(
    String currentBrokerHost,
    boolean isOldBroker
) {}
