package com.aeg.core.security;

public enum Role {
    ADMIN,
    /** Operaciones de distribuidora en panel + inspecciones; no crea servicios técnicos. */
    DISTRIBUTOR,
    /** Técnico firmante: mismo panel que distribuidor + único rol que puede firmar ST. */
    TECHNICIAN,
    /** Personal de centro de servicio: crea servicios técnicos e inspecciones en libro fiscal. */
    SERVICE_CENTER,
    /** Auditor SENIAT: solo portal de libros fiscales, lectura global sin altas. */
    SENIAT;

    public static boolean isDistributorScoped(Role role) {
        return role == DISTRIBUTOR || role == TECHNICIAN;
    }

    public static boolean isServiceCenter(Role role) {
        return role == SERVICE_CENTER;
    }

    public static boolean isPanelRole(Role role) {
        return role == ADMIN || isDistributorScoped(role);
    }

    public static boolean canWriteAnnualInspection(Role role) {
        return role == ADMIN || isDistributorScoped(role) || isServiceCenter(role);
    }

    public static boolean canWriteTechnicalService(Role role) {
        return role == ADMIN || isServiceCenter(role);
    }

    public static boolean canSignTechnicalService(Role role) {
        return role == TECHNICIAN;
    }

    public static boolean canBeInspectionInspector(Role role) {
        return isDistributorScoped(role) || isServiceCenter(role);
    }
}
