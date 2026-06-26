package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void distributorScopedRolesIncludeDistributorAndTechnician() {
        assertThat(Role.isDistributorScoped(Role.DISTRIBUTOR)).isTrue();
        assertThat(Role.isDistributorScoped(Role.TECHNICIAN)).isTrue();
        assertThat(Role.isDistributorScoped(Role.SERVICE_CENTER)).isFalse();
    }

    @Test
    void technicalServiceWriteIsLimitedToAdminAndServiceCenter() {
        assertThat(Role.canWriteTechnicalService(Role.ADMIN)).isTrue();
        assertThat(Role.canWriteTechnicalService(Role.SERVICE_CENTER)).isTrue();
        assertThat(Role.canWriteTechnicalService(Role.DISTRIBUTOR)).isFalse();
        assertThat(Role.canWriteTechnicalService(Role.TECHNICIAN)).isFalse();
    }

    @Test
    void annualInspectionWriteIncludesDistributorTechnicianAndServiceCenter() {
        assertThat(Role.canWriteAnnualInspection(Role.DISTRIBUTOR)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.TECHNICIAN)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.SERVICE_CENTER)).isTrue();
        assertThat(Role.canWriteAnnualInspection(Role.SENIAT)).isFalse();
    }

    @Test
    void onlyTechnicianCanSignTechnicalService() {
        assertThat(Role.canSignTechnicalService(Role.TECHNICIAN)).isTrue();
        assertThat(Role.canSignTechnicalService(Role.DISTRIBUTOR)).isFalse();
        assertThat(Role.canSignTechnicalService(Role.SERVICE_CENTER)).isFalse();
    }
}
