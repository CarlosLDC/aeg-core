package com.aeg.core.mqtt.dto;

public record ToolsReportZResponse(boolean success, String message, ToolsReportZDataDto report) {

    public static ToolsReportZResponse ok(ToolsReportZDataDto report) {
        return new ToolsReportZResponse(true, null, report);
    }
}
