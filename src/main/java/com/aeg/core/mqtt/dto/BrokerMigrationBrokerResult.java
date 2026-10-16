package com.aeg.core.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrokerMigrationBrokerResult(
    String currentBrokerHost,
    @JsonProperty("isOldBroker") boolean isOldBroker
) {}
