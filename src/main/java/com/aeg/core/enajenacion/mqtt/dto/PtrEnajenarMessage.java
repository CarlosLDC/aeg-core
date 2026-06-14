package com.aeg.core.enajenacion.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PtrEnajenarMessage(
        String cmd,
        JsonNode data) {

    public String ptrReg() {
        return data != null && data.hasNonNull("ptrReg") ? data.get("ptrReg").asText() : null;
    }

    public String macAddr() {
        return data != null && data.hasNonNull("macAddr") ? data.get("macAddr").asText() : null;
    }
}
