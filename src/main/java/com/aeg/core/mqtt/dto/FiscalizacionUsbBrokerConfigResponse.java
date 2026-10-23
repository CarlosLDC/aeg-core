package com.aeg.core.mqtt.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record FiscalizacionUsbBrokerConfigResponse(
        JsonNode payload,
        String newBrokerHost,
        String legacyBrokerHost) {
}
