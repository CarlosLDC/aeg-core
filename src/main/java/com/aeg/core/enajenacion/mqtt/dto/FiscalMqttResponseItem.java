package com.aeg.core.enajenacion.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FiscalMqttResponseItem(
        String cmd,
        Integer code,
        Integer dataD) {
}
