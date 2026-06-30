package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnnualInspectionVerifyQrRequest(
        @NotNull Long printerId,
        @NotBlank String qrCodigo,
        @NotBlank String registroImpresora) {
}
