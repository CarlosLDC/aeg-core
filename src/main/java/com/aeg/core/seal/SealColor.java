package com.aeg.core.seal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SealColor {
    AZUL("azul"),
    MORADO("morado"),
    VERDE("verde"),
    VERDE_NEON("verde_neon");

    private final String value;

    SealColor(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static SealColor fromValue(String v) {
        if (v == null) return null;
        for (SealColor c : values()) {
            if (c.value.equalsIgnoreCase(v)) return c;
        }
        throw new IllegalArgumentException("Unknown SealColor: " + v);
    }
}
