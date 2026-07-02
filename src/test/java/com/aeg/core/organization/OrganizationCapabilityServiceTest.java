package com.aeg.core.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.OrganizationType;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;

import static org.mockito.Mockito.when;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrganizationCapabilityServiceTest {

	@Mock
	private SecurityScopeService securityScope;

	@Mock
	private BranchRepository branchRepository;

	@Mock
	private DistributorRepository distributorRepository;

	@InjectMocks
	private OrganizationCapabilityService service;

	@Test
	void factoryHasAllCapabilities() {
		Company company = factoryCompany();
		Branch branch = standardBranch();

		assertThat(service.hasCapability(
				service.resolve(company, branch), OrgCapability.ASSIGN_TO_DISTRIBUTOR)).isTrue();
		assertThat(service.hasCapability(
				service.resolve(company, branch), OrgCapability.ENAJENAR)).isTrue();
		assertThat(service.hasCapability(
				service.resolve(company, branch), OrgCapability.WRITE_TECHNICAL_SERVICE)).isTrue();
		assertThat(service.hasCapability(
				service.resolve(company, branch), OrgCapability.WRITE_ANNUAL_INSPECTION)).isTrue();
	}

	@Test
	void serviceCenterCannotAssignPrintersOrWriteBeyondScope() {
		Company company = standardCompany();
		Branch branch = serviceCenterBranch();

		var profile = service.resolve(company, branch);
		assertThat(service.hasCapability(profile, OrgCapability.ASSIGN_TO_DISTRIBUTOR)).isFalse();
		assertThat(service.hasCapability(profile, OrgCapability.ENAJENAR)).isTrue();
		assertThat(service.hasCapability(profile, OrgCapability.WRITE_TECHNICAL_SERVICE)).isTrue();
		assertThat(service.hasCapability(profile, OrgCapability.WRITE_ANNUAL_INSPECTION)).isTrue();
	}

	@Test
	void distributorCannotAssignOrWriteTechnicalServices() {
		Company company = standardCompany();
		Branch branch = distributorBranch();

		var profile = service.resolve(company, branch);
		assertThat(service.hasCapability(profile, OrgCapability.ASSIGN_TO_DISTRIBUTOR)).isFalse();
		assertThat(service.hasCapability(profile, OrgCapability.WRITE_TECHNICAL_SERVICE)).isFalse();
		assertThat(service.hasCapability(profile, OrgCapability.ENAJENAR)).isTrue();
		assertThat(service.hasCapability(profile, OrgCapability.WRITE_ANNUAL_INSPECTION)).isTrue();
	}

	@Test
	void resolveActorProfile_mapsTechnicianWithBranchToServiceCenterProfile() {
		Company company = standardCompany();
		Branch branch = serviceCenterBranch();
		branch.setId(12L);
		branch.setCompany(company);

		User technician = User.builder()
				.role(Role.TECHNICIAN)
				.branchId(12L)
				.build();

		when(securityScope.currentUser()).thenReturn(technician);
		when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));

		assertThat(service.resolveActorProfile()).isEqualTo(OrganizationProfile.SERVICE_CENTER);
	}

	@Test
	void resolve_usesLegacyDistributorFlagWhenOrganizationRoleNone() {
		Company company = standardCompany();
		Branch branch = new Branch();
		branch.setOrganizationRole(BranchOrganizationRole.NONE);
		branch.setIsDistributor(true);
		branch.setIsServiceCenter(false);

		assertThat(service.resolve(company, branch)).isEqualTo(OrganizationProfile.DISTRIBUTOR);
		assertThat(service.hasCapability(
				service.resolve(company, branch), OrgCapability.ENAJENAR)).isTrue();
	}

	@Test
	void resolveActorProfile_distributorUserDefaultsToDistributorWhenBranchRoleUnset() {
		Company company = standardCompany();
		Branch branch = new Branch();
		branch.setId(5L);
		branch.setCompany(company);
		branch.setOrganizationRole(BranchOrganizationRole.NONE);
		branch.setIsDistributor(false);

		com.aeg.core.distributor.Distributor distributor = new com.aeg.core.distributor.Distributor();
		distributor.setId(7L);
		distributor.setBranch(branch);

		User distributorUser = User.builder()
				.role(Role.DISTRIBUTOR)
				.distributorId(7L)
				.build();

		when(securityScope.currentUser()).thenReturn(distributorUser);
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		assertThat(service.resolveActorProfile()).isEqualTo(OrganizationProfile.DISTRIBUTOR);
		service.assertActorCan(OrgCapability.ENAJENAR);
	}

	@Test
	void assertCapabilityThrowsWhenDenied() {
		assertThatThrownBy(() -> service.assertCapability(
				OrganizationProfile.DISTRIBUTOR, OrgCapability.ASSIGN_TO_DISTRIBUTOR))
				.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	private static Company factoryCompany() {
		Company company = new Company();
		company.setOrganizationType(OrganizationType.FACTORY);
		return company;
	}

	private static Company standardCompany() {
		Company company = new Company();
		company.setOrganizationType(OrganizationType.STANDARD);
		return company;
	}

	private static Branch standardBranch() {
		Branch branch = new Branch();
		branch.setOrganizationRole(BranchOrganizationRole.NONE);
		return branch;
	}

	private static Branch serviceCenterBranch() {
		Branch branch = new Branch();
		branch.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);
		return branch;
	}

	private static Branch distributorBranch() {
		Branch branch = new Branch();
		branch.setOrganizationRole(BranchOrganizationRole.DISTRIBUTOR);
		return branch;
	}
}
