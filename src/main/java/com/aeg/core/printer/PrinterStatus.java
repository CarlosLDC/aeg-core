package com.aeg.core.printer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PrinterStatus {
    DE_FABRICA("de_fabrica"),
    SIN_ASIGNAR("sin_asignar"),
    ASIGNADA("asignada"),
    EN_CONSIGNACION("en_consignacion"),
    ENAJENADA("enajenada"),
    A_CREDITO("a_credito"),
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

    /** Impresoras asignadas, en consignación o en laboratorio pueden iniciar enajenación MQTT. */
    public boolean isEligibleForMqttEnajenacion() {
        return this == ASIGNADA || this == EN_CONSIGNACION || this == LABORATORIO;
    }

    /** Impresoras enajenadas al cliente (pagadas o a crédito). */
    public boolean isDisposed() {
        return this == ENAJENADA || this == A_CREDITO;
    }

    @JsonCreator
    public static PrinterStatus fromValue(String v) {
        if (v == null) return null;
        v = v.trim().toLowerCase();
        if (v.isBlank()) {
            return LABORATORIO;
        }
        if ("activo".equals(v)) {
            return ASIGNADA;
        }
        if ("inactivo".equals(v)) {
            return DESINCORPORADA;
        }
        if ("inicializada".equals(v)) {
            return SIN_ASIGNAR;
        }
        if ("de_demostracion".equals(v)) {
            return LABORATORIO;
        }
        for (PrinterStatus s : values()) {
            if (s.value.equalsIgnoreCase(v) || s.name().equalsIgnoreCase(v)) return s;
        }
        // Compatibilidad: datos legacy o valores externos no deben romper lectura.
        return LABORATORIO;
    }
}
