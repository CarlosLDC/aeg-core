package com.aeg.core.fiscalizacion.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PtrFiscalizarMessage(String cmd, JsonNode data) {

    public String ptrReg() {
        return text("ptrReg");
    }

    public String macAddr() {
        return text("macAddr");
    }

    public String precintoNro() {
        return text("PrecintoNro");
    }

    public String precintoColor() {
        return text("PrecintoColor");
    }

    public String firmwareVersion() {
        return text("firmwareVersion");
    }

    public String model() {
        return text("model");
    }

    private String text(String field) {
        return data != null && data.hasNonNull(field) ? data.get(field).asText() : null;
    }
}
