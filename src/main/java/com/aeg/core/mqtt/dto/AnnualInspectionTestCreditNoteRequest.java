package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnnualInspectionTestCreditNoteRequest(
        @NotNull Long printerId,
        @NotNull Integer numeroFacturaPrueba,
        @NotBlank String registroImpresora,
        String productDescription) {}
