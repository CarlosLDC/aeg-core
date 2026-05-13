package com.aeg.core.company;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContributorType {
    ORDINARIO("ordinario"),
    ESPECIAL("especial"),
    FORMAL("formal");

    private final String value;

    ContributorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ContributorType fromString(String v) {
        if (v == null) return null;
        for (ContributorType t : ContributorType.values()) {
            if (t.value.equalsIgnoreCase(v) || t.name().equalsIgnoreCase(v)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown contributorType: " + v);
    }
}
