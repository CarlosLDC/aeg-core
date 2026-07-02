package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
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

	@BeforeEach
	void setUp() {
		service = new SecurityScopeService(
				branchRepository,
				clientRepository,
				distributorRepository,
				printerRepository,
				sealRepository,
				serviceCenterRepository);
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
}
