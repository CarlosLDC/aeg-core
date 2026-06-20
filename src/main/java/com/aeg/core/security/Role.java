package com.aeg.core.security;

public enum Role {
    ADMIN,
    /** Usuario operativo de campo y panel por distribuidora. */
    TECHNICIAN,
    /** Auditor SENIAT: solo portal de libros fiscales, lectura global sin altas. */
    SENIAT
}
