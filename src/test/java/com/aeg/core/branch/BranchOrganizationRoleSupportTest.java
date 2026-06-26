package com.aeg.core.branch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.aeg.core.company.Company;
import com.aeg.core.company.OrganizationType;

class BranchOrganizationRoleSupportTest {

	@Test
	void rejectsOperationalRoleOnFactoryCompany() {
		Company company = new Company();
		company.setOrganizationType(OrganizationType.FACTORY);

		assertThatThrownBy(() -> BranchOrganizationRoleSupport.assertOperationalRoleAllowed(
				company, BranchOrganizationRole.DISTRIBUTOR))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("fábrica");
	}

	@Test
	void rejectsConflictingServiceCenterAndDistributor() {
		Branch branch = new Branch();
		branch.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);

		assertThatThrownBy(() -> BranchOrganizationRoleSupport.assertNotConflictingRole(
				branch, BranchOrganizationRole.DISTRIBUTOR))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
