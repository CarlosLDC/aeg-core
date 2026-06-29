package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void distributorScopedRoleIsDistributorOnly() {
        assertThat(Role.isDistributorScoped(Role.DISTRIBUTOR)).isTrue();
        assertThat(Role.isDistributorScoped(Role.TECHNICIAN)).isFalse();
        assertThat(Role.isDistributorScoped(Role.SERVICE_CENTER)).isFalse();
    }

    @Test
    void serviceCenterStaffRequiresTechnicianWithBranch() {
        User technician = User.builder().role(Role.TECHNICIAN).branchId(12L).build();
        User legacy = User.builder().role(Role.SERVICE_CENTER).branchId(12L).build();
        User distributorTech = User.builder().role(Role.TECHNICIAN).distributorId(7L).build();

        assertThat(Role.isServiceCenterStaff(technician)).isTrue();
        assertThat(Role.isServiceCenterStaff(legacy)).isTrue();
        assertThat(Role.isServiceCenterStaff(distributorTech)).isFalse();
    }

    @Test
    void technicalServiceWriteIsLimitedToAdminAndServiceCenterTechnicians() {
        assertThat(Role.canWriteTechnicalService(Role.ADMIN)).isTrue();
        assertThat(Role.canWriteTechnicalService(Role.TECHNICIAN)).isTrue();
        assertThat(Role.canWriteTechnicalService(Role.SERVICE_CENTER)).isTrue();
        assertThat(Role.canWriteTechnicalService(Role.DISTRIBUTOR)).isFalse();
    }

    @Test
    void annualInspectionWriteIncludesDistributorAndTechnician() {
        assertThat(Role.canWriteAnnualInspection(Role.ADMIN)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.DISTRIBUTOR)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.TECHNICIAN)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.SERVICE_CENTER)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.SENIAT)).isFalse();
    }

    @Test
    void inspectionInspectorMatchesAnnualInspectionWriters() {
        assertThat(Role.canBeInspectionInspector(Role.ADMIN)).isTrue();
        assertThat(Role.canBeInspectionInspector(Role.DISTRIBUTOR)).isTrue();
        assertThat(Role.canBeInspectionInspector(Role.TECHNICIAN)).isTrue();
        assertThat(Role.canBeInspectionInspector(Role.SERVICE_CENTER)).isTrue();
        assertThat(Role.canBeInspectionInspector(Role.SENIAT)).isFalse();
    }

    @Test
    void adminAndServiceCenterTechniciansCanSignTechnicalService() {
        assertThat(Role.canSignTechnicalService(Role.ADMIN)).isTrue();
        assertThat(Role.canSignTechnicalService(Role.TECHNICIAN)).isTrue();
        assertThat(Role.canSignTechnicalService(Role.DISTRIBUTOR)).isFalse();
        assertThat(Role.canSignTechnicalService(Role.SERVICE_CENTER)).isTrue();
    }

    @Test
    void panelRolesExcludeServiceCenterTechnicians() {
        assertThat(Role.isPanelRole(Role.ADMIN)).isTrue();
        assertThat(Role.isPanelRole(Role.DISTRIBUTOR)).isTrue();
        assertThat(Role.isPanelRole(Role.TECHNICIAN)).isFalse();
    }
}
