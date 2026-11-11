package com.aeg.core.mqtt.dto;

public record ToolsMqttClockResponse(
        boolean success,
        String message,
        String dateTime) {

    public static ToolsMqttClockResponse ok(String dateTime) {
        return new ToolsMqttClockResponse(true, null, dateTime);
    }

    public static ToolsMqttClockResponse error(String message) {
        return new ToolsMqttClockResponse(false, message, null);
    }
}
