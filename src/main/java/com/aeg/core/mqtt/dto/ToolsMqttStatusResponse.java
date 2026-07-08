package com.aeg.core.mqtt.dto;

public record ToolsMqttStatusResponse(
        boolean success,
        String message,
        String seniatStatus,
        ToolsMqttAdditionalInfoDto additionalInfo,
        Integer code) {

    public static ToolsMqttStatusResponse ok(String seniatStatus, ToolsMqttAdditionalInfoDto additionalInfo) {
        return new ToolsMqttStatusResponse(true, null, seniatStatus, additionalInfo, null);
    }

    public static ToolsMqttStatusResponse error(String message, Integer code) {
        return new ToolsMqttStatusResponse(false, message, null, null, code);
    }
}
