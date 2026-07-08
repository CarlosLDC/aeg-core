package com.aeg.core.mqtt.dto;

public record ToolsReprintResponse(
        boolean success,
        String message,
        String escPosContent,
        String mode,
        String docType,
        Integer number) {

    public static ToolsReprintResponse ok(String escPosContent, String mode, String docType, Integer number) {
        return new ToolsReprintResponse(true, null, escPosContent, mode, docType, number);
    }

    public static ToolsReprintResponse ack(String mode, String docType, Integer number) {
        return new ToolsReprintResponse(true, "Comando enviado a la impresora.", null, mode, docType, number);
    }
}
