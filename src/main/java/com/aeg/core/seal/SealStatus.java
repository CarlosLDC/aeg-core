package com.aeg.core.seal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SealStatus {
    DISPONIBLE("disponible"),
    EN_IMPRESORA("en_impresora"),
    SUSTITUIDO("sustituido");

    private final String value;

    SealStatus(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static SealStatus fromValue(String v) {
        if (v == null) return null;
        for (SealStatus s : values()) {
            if (s.value.equalsIgnoreCase(v)) return s;
        }
        throw new IllegalArgumentException("Unknown SealStatus: " + v);
    }
}
