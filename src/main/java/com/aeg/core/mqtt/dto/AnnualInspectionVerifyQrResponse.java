package com.aeg.core.mqtt.dto;

public record AnnualInspectionVerifyQrResponse(
        boolean valido,
        String registro,
        String mac,
        String fecha) {
}
