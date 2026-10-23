package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;

public record FiscalizacionUsbPrecheckRequest(
        @NotBlank String ptrReg,
        @NotBlank String precintoNro,
        @NotBlank String precintoColor,
        String mac) {
}
