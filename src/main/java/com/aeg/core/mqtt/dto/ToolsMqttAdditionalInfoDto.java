package com.aeg.core.mqtt.dto;

public record ToolsMqttAdditionalInfoDto(
        String wifiNetwork,
        String ipAddress,
        int lastZReport,
        Integer lastZTransmitted,
        int daysSinceLastTx) {}
