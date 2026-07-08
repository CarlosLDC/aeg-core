package com.aeg.core.mqtt.dto;

public record ToolsMqttSimpleResponse(boolean success, String message) {

    public static ToolsMqttSimpleResponse ok(String message) {
        return new ToolsMqttSimpleResponse(true, message);
    }

    public static ToolsMqttSimpleResponse error(String message) {
        return new ToolsMqttSimpleResponse(false, message);
    }
}
