package com.aeg.core.security;

public enum Role {
    ADMIN,
    DISTRIBUTOR,
    TECHNICIAN,
    SERVICE_CENTER,
    /** Solo lectura global (libro fiscal / auditoría SENIAT). */
    SENIAT
}
