package com.aeg.core.printer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrinterStatus {
    DE_DEMOSTRACION("de_demostracion"),
    DE_FABRICA("de_fabrica"),
    INICIALIZADA("inicializada"),
    ASIGNADA("asignada"),
    ENAJENADA("enajenada"),
    DESINCORPORADA("desincorporada"),
    LABORATORIO("laboratorio");

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
        v = v.trim().toLowerCase();
        if ("activo".equals(v)) {
            return ASIGNADA;
        }
        if ("inactivo".equals(v)) {
            return DESINCORPORADA;
        }
        for (PrinterStatus s : values()) {
            if (s.value.equalsIgnoreCase(v) || s.name().equalsIgnoreCase(v)) return s;
        }
        throw new IllegalArgumentException("Invalid printer status: " + v);
    }
}
