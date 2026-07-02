package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.security.UserController.UserRegistrationRequest;
import com.aeg.core.security.UserController.UserUpdateRequest;
import com.aeg.core.servicecenter.ServiceCenterRepository;

@ExtendWith(MockitoExtension.class)
class UserRoleAssignmentServiceTest {

	@Mock
	private DistributorRepository distributorRepository;

	@Mock
	private BranchRepository branchRepository;

	@Mock
	private ServiceCenterRepository serviceCenterRepository;

	private UserRoleAssignmentService service;

	@BeforeEach
	void setUp() {
		service = new UserRoleAssignmentService(
				distributorRepository,
				branchRepository,
				serviceCenterRepository);
	}

	@Test
	void resolveForUpdate_adminToDistributor_ignoresStaleBranchId() {
		User existing = User.builder()
				.id(1L)
				.role(Role.ADMIN)
				.branchId(12L)
				.nationalId("V12345678")
				.build();

		Distributor distributor = distributorWithOrgRole(7L, BranchOrganizationRole.DISTRIBUTOR);
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		UserUpdateRequest request = new UserUpdateRequest();
		request.setRole("DISTRIBUTOR");
		request.setDistributorId(7L);
		request.setBranchId(null);
		request.setNationalId("V12345678");

		var resolution = service.resolveForUpdate(request, existing);

		assertThat(resolution.hasError()).isFalse();
		assertThat(resolution.role()).isEqualTo(Role.DISTRIBUTOR);
	}

	@Test
	void resolveForUpdate_adminToDistributorWithoutDistributorId_returnsBadRequest() {
		User existing = User.builder()
				.id(1L)
				.role(Role.ADMIN)
				.nationalId("V12345678")
				.build();

		UserUpdateRequest request = new UserUpdateRequest();
		request.setRole("DISTRIBUTOR");
		request.setNationalId("V12345678");

		var resolution = service.resolveForUpdate(request, existing);

		assertThat(resolution.hasError()).isTrue();
		assertThat(resolution.errorStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void resolveForUpdate_adminWithDistributorIdInRequest_returnsBadRequest() {
		User existing = User.builder()
				.id(1L)
				.role(Role.ADMIN)
				.nationalId("V12345678")
				.build();

		UserUpdateRequest request = new UserUpdateRequest();
		request.setRole("ADMIN");
		request.setDistributorId(7L);

		var resolution = service.resolveForUpdate(request, existing);

		assertThat(resolution.hasError()).isTrue();
		assertThat(resolution.errorStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void resolveForCreate_distributorWithStaleBranchMetadata_succeeds() {
		Branch branch = new Branch();
		branch.setId(99L);
		branch.setOrganizationRole(BranchOrganizationRole.NONE);
		branch.setIsDistributor(false);
		Distributor distributor = new Distributor();
		distributor.setId(7L);
		distributor.setBranch(branch);
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		UserRegistrationRequest request = new UserRegistrationRequest();
		request.setRole("DISTRIBUTOR");
		request.setDistributorId(7L);
		request.setNationalId("V12345678");

		var resolution = service.resolveForCreate(request);

		assertThat(resolution.hasError()).isFalse();
		assertThat(resolution.role()).isEqualTo(Role.DISTRIBUTOR);
	}

	@Test
	void resolveForCreate_distributorRole_ignoresBranchIdInRequest() {
		Distributor distributor = distributorWithOrgRole(7L, BranchOrganizationRole.DISTRIBUTOR);
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		UserRegistrationRequest request = new UserRegistrationRequest();
		request.setRole("DISTRIBUTOR");
		request.setDistributorId(7L);
		request.setBranchId(12L);
		request.setNationalId("V12345678");

		var resolution = service.resolveForCreate(request);

		assertThat(resolution.hasError()).isFalse();
		assertThat(resolution.role()).isEqualTo(Role.DISTRIBUTOR);
	}

	private static Distributor distributorWithOrgRole(Long id, BranchOrganizationRole orgRole) {
		Branch branch = new Branch();
		branch.setId(99L);
		branch.setOrganizationRole(orgRole);
		branch.setIsDistributor(orgRole == BranchOrganizationRole.DISTRIBUTOR);
		Distributor distributor = new Distributor();
		distributor.setId(id);
		distributor.setBranch(branch);
		return distributor;
	}
}
