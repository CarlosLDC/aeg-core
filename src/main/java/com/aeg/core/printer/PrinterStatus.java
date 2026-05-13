package com.aeg.core.printer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrinterStatus {
    LABORATORIO("laboratorio"),
    ACTIVO("activo"),
    INACTIVO("inactivo");

    private final String value;

    PrinterStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PrinterStatus fromValue(String v) {
        if (v == null) return null;
        for (PrinterStatus s : values()) {
            if (s.value.equalsIgnoreCase(v)) return s;
        }
        throw new IllegalArgumentException("Unknown PrinterStatus: " + v);
    }
}
