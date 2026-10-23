package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotBlank;

public record FiscalizacionUsbConfigureBrokerRequest(@NotBlank String mac) {
}
