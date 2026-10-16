package com.aeg.core.mqtt.dto;

public record ToolsMqttStatusResponse(
        boolean success,
        String message,
        String seniatStatus,
        ToolsMqttAdditionalInfoDto additionalInfo,
        Integer code,
        Boolean legacyBroker) {

    public ToolsMqttStatusResponse(
            boolean success,
            String message,
            String seniatStatus,
            ToolsMqttAdditionalInfoDto additionalInfo,
            Integer code) {
        this(success, message, seniatStatus, additionalInfo, code, false);
    }

    public static ToolsMqttStatusResponse ok(String seniatStatus, ToolsMqttAdditionalInfoDto additionalInfo) {
        return new ToolsMqttStatusResponse(true, null, seniatStatus, additionalInfo, null, false);
    }

    public static ToolsMqttStatusResponse error(String message, Integer code) {
        return new ToolsMqttStatusResponse(false, message, null, null, code, false);
    }

    public ToolsMqttStatusResponse withLegacyBroker(boolean isLegacyBroker) {
        return new ToolsMqttStatusResponse(success, message, seniatStatus, additionalInfo, code, isLegacyBroker);
    }
}
