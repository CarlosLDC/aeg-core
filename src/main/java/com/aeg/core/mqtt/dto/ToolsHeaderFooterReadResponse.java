package com.aeg.core.mqtt.dto;

public record ToolsHeaderFooterReadResponse(boolean success, String message, String content) {

    public static ToolsHeaderFooterReadResponse ok(String content) {
        return new ToolsHeaderFooterReadResponse(true, null, content);
    }
}
