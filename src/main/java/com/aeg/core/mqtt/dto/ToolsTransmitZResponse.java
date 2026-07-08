package com.aeg.core.mqtt.dto;

public record ToolsTransmitZResponse(
        boolean success,
        String message,
        Integer lastTransmittedZ,
        boolean seniatUnavailable) {

    public static ToolsTransmitZResponse ok(int lastTransmittedZ) {
        return new ToolsTransmitZResponse(true, null, lastTransmittedZ, false);
    }

    public static ToolsTransmitZResponse seniatUnavailable(String message) {
        return new ToolsTransmitZResponse(false, message, null, true);
    }
}
