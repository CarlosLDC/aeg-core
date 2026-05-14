package com.aeg.core.printer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceType {
    INTERNO("interno"),
    EXTERNO("externo");

    private final String value;

    DeviceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DeviceType fromValue(String v) {
        if (v == null) return null;
        v = v.trim();
        for (DeviceType d : values()) {
            if (d.value.equalsIgnoreCase(v) || d.name().equalsIgnoreCase(v)) return d;
        }
        return null;
    }
}
