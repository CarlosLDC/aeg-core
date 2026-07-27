package com.aeg.core.mqtt.dto;

public record ToolsReportXResponse(
        boolean success,
        String message,
        String escPosContent) {

    public static ToolsReportXResponse ok(String escPosContent) {
        return new ToolsReportXResponse(true, null, escPosContent);
    }

    public static ToolsReportXResponse printed() {
        return new ToolsReportXResponse(true, "Reporte X enviado a imprimir.", null);
    }
}
