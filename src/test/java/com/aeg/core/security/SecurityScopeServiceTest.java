package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.ClientBranchLinkPolicy;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.servicecenter.ServiceCenterRepository;

@ExtendWith(MockitoExtension.class)
class SecurityScopeServiceTest {

	@Mock
	private BranchRepository branchRepository;

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private DistributorRepository distributorRepository;

	@Mock
	private PrinterRepository printerRepository;

	@Mock
	private SealRepository sealRepository;

	@Mock
	private ServiceCenterRepository serviceCenterRepository;

	private SecurityScopeService service;
	private ClientBranchLinkPolicy clientBranchLinkPolicy;

	@BeforeEach
	void setUp() {
		clientBranchLinkPolicy = new ClientBranchLinkPolicy(
				branchRepository,
				clientRepository,
				distributorRepository,
				serviceCenterRepository);
		service = new SecurityScopeService(
				branchRepository,
				clientRepository,
				distributorRepository,
				printerRepository,
				sealRepository,
				serviceCenterRepository,
				clientBranchLinkPolicy);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void resolveBranchScope_distributor_includesOwnBranchAndClientBranches() {
		User distributorUser = User.builder()
				.role(Role.DISTRIBUTOR)
				.distributorId(7L)
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(distributorUser, null, List.of()));

		Branch clientBranch = new Branch();
		clientBranch.setId(20L);
		Branch distributorBranch = new Branch();
		distributorBranch.setId(99L);
		Distributor distributor = new Distributor();
		distributor.setId(7L);
		distributor.setBranch(distributorBranch);

		when(branchRepository.findBranchesByDistributorId(7L)).thenReturn(List.of(clientBranch));
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		var scope = service.resolveBranchScope();

		assertThat(scope.visibility()).isEqualTo(BranchScope.Visibility.SCOPED);
		assertThat(scope.branchIds()).containsExactlyInAnyOrder(20L, 99L);
	}

	@Test
	void resolveBranchScope_distributor_withNoClients_includesOnlyOwnBranch() {
		User distributorUser = User.builder()
				.role(Role.DISTRIBUTOR)
				.distributorId(7L)
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(distributorUser, null, List.of()));

		Branch distributorBranch = new Branch();
		distributorBranch.setId(99L);
		Distributor distributor = new Distributor();
		distributor.setId(7L);
		distributor.setBranch(distributorBranch);

		when(branchRepository.findBranchesByDistributorId(7L)).thenReturn(List.of());
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		var scope = service.resolveBranchScope();

		assertThat(scope.visibility()).isEqualTo(BranchScope.Visibility.SCOPED);
		assertThat(scope.branchIds()).containsExactly(99L);
	}

	@Test
	void assertBranchReadable_rejectsServiceCenterAndForeignDistributorBranches() {
		User distributorUser = User.builder()
				.role(Role.DISTRIBUTOR)
				.distributorId(7L)
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(distributorUser, null, List.of()));

		Distributor ownDist = new Distributor();
		ownDist.setId(7L);
		Branch ownBranch = new Branch();
		ownBranch.setId(99L);
		ownDist.setBranch(ownBranch);
		when(branchRepository.findBranchesByDistributorId(7L)).thenReturn(List.of());
		when(distributorRepository.findById(7L)).thenReturn(Optional.of(ownDist));

		Branch serviceCenterBranch = new Branch();
		serviceCenterBranch.setId(50L);
		serviceCenterBranch.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);
		when(branchRepository.findById(50L)).thenReturn(Optional.of(serviceCenterBranch));

		assertThatThrownBy(() -> service.assertBranchReadable(50L))
				.isInstanceOf(AccessDeniedException.class);

		Branch foreignDistBranch = new Branch();
		foreignDistBranch.setId(60L);
		foreignDistBranch.setOrganizationRole(BranchOrganizationRole.NONE);
		Distributor foreignDist = new Distributor();
		foreignDist.setId(99L);
		when(branchRepository.findById(60L)).thenReturn(Optional.of(foreignDistBranch));
		when(serviceCenterRepository.findByBranch_Id(60L)).thenReturn(Optional.empty());
		when(distributorRepository.findByBranch_Id(60L)).thenReturn(Optional.of(foreignDist));

		assertThatThrownBy(() -> service.assertBranchReadable(60L))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void assertTechnicalServiceSignerInScope_rejectsAdminOrOtherBranchTechnicianForNonAdmin() {
		User tech = User.builder()
				.id(10L)
				.role(Role.TECHNICIAN)
				.branchId(5L)
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(tech, null, List.of()));

		User adminSigner = User.builder().id(1L).role(Role.ADMIN).build();
		assertThatThrownBy(() -> service.assertTechnicalServiceSignerInScope(adminSigner))
				.isInstanceOf(AccessDeniedException.class);

		User otherBranchTech = User.builder().id(11L).role(Role.TECHNICIAN).branchId(6L).build();
		assertThatThrownBy(() -> service.assertTechnicalServiceSignerInScope(otherBranchTech))
				.isInstanceOf(AccessDeniedException.class);

		User sameBranchTech = User.builder().id(12L).role(Role.TECHNICIAN).branchId(5L).build();
		service.assertTechnicalServiceSignerInScope(sameBranchTech);
	}
}
