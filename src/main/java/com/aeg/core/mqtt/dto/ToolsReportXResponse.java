package com.aeg.core.mqtt.dto;

public record ToolsReportXResponse(
        boolean success,
        String message,
        String escPosContent) {

    public static ToolsReportXResponse ok(String escPosContent) {
        return new ToolsReportXResponse(true, null, escPosContent);
    }
}
