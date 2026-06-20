package com.aeg.core.security;

public enum Role {
    ADMIN,
    DISTRIBUTOR,
    TECHNICIAN,
    SERVICE_CENTER,
    /** Auditor SENIAT: solo portal de libros fiscales, lectura global sin altas. */
    SENIAT
}
