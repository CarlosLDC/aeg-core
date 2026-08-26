package com.aeg.core.enajenacion.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FiscalMqttResponseItem(
        String cmd,
        Integer code,
        Integer dataD,
        String dataS,
        String llaveEncrip) {

    public FiscalMqttResponseItem(String cmd, Integer code, Integer dataD) {
        this(cmd, code, dataD, null, null);
    }

    public FiscalMqttResponseItem(String cmd, Integer code, Integer dataD, String dataS) {
        this(cmd, code, dataD, dataS, null);
    }
}
