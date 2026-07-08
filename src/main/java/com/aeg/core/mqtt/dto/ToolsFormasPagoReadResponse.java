package com.aeg.core.mqtt.dto;

import java.util.List;

public record ToolsFormasPagoReadResponse(boolean success, String message, List<ToolsFormasPagoItemDto> formasPago) {

    public static ToolsFormasPagoReadResponse ok(List<ToolsFormasPagoItemDto> formasPago) {
        return new ToolsFormasPagoReadResponse(true, null, formasPago);
    }
}
