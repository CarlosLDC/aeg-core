package com.aeg.core.mqtt.dto;

public record BrokerMigrationFirmwareResult(
    String firmwareVersion,
    boolean needsUpdate
) {}
