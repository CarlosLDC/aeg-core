package com.aeg.core.mqtt.dto;

import java.util.List;

public record FiscalizacionUsbPrecheckResponse(boolean ok, List<String> errors) {
}
