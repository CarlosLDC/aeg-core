package com.aeg.core.security;

public enum Role {
    ADMIN,
    /** Operaciones de distribuidora en panel + inspecciones; no crea servicios técnicos. */
    DISTRIBUTOR,
    /** Personal de centro de servicio: libro fiscal, servicios técnicos e inspecciones. */
    TECHNICIAN,
    /** @deprecated Migrado a {@link #TECHNICIAN}; conservado solo para datos legacy. */
    @Deprecated
    SERVICE_CENTER,
    /** Auditor SENIAT: solo portal de libros fiscales, lectura global sin altas. */
    SENIAT;

    public static boolean isDistributorScoped(Role role) {
        return role == DISTRIBUTOR;
    }

    /** @deprecated Use {@link #isServiceCenterStaff(User)} */
    @Deprecated
    public static boolean isServiceCenter(Role role) {
        return role == SERVICE_CENTER || role == TECHNICIAN;
    }

    public static boolean isServiceCenterStaff(User user) {
        return user != null
                && (user.getRole() == TECHNICIAN || user.getRole() == SERVICE_CENTER)
                && user.getBranchId() != null;
    }

    public static boolean isPanelRole(Role role) {
        return role == ADMIN || isDistributorScoped(role);
    }

    public static boolean canWriteAnnualInspection(Role role) {
        return role == ADMIN || isDistributorScoped(role) || role == TECHNICIAN || role == SERVICE_CENTER;
    }

    public static boolean canWriteTechnicalService(Role role) {
        return role == ADMIN || role == TECHNICIAN || role == SERVICE_CENTER;
    }

    public static boolean canSignTechnicalService(Role role) {
        return role == ADMIN || role == TECHNICIAN || role == SERVICE_CENTER;
    }

    public static boolean canBeInspectionInspector(Role role) {
        return isDistributorScoped(role) || role == TECHNICIAN || role == SERVICE_CENTER;
    }
}
