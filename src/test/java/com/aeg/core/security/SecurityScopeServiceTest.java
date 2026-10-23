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
	void findVisibleSeals_distributor_includesInstalledAndUnassignedAvailableSeals() {
		User distributorUser = User.builder()
				.role(Role.DISTRIBUTOR)
				.distributorId(7L)
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(distributorUser, null, List.of()));

		com.aeg.core.printer.Printer printer = new com.aeg.core.printer.Printer();
		printer.setId(500L);
		com.aeg.core.seal.Seal installedSeal = new com.aeg.core.seal.Seal();
		installedSeal.setId(1L);
		installedSeal.setSerial("S-INSTALLED");
		installedSeal.setStatus(com.aeg.core.seal.SealStatus.EN_IMPRESORA);

		com.aeg.core.seal.Seal availableSeal = new com.aeg.core.seal.Seal();
		availableSeal.setId(2L);
		availableSeal.setSerial("S-AVAILABLE");
		availableSeal.setStatus(com.aeg.core.seal.SealStatus.DISPONIBLE);

		when(printerRepository.findByDistributor_Id(7L)).thenReturn(List.of(printer));
		when(sealRepository.findByPrinter_IdIn(List.of(500L))).thenReturn(List.of(installedSeal));
		when(sealRepository.findByStatusAndPrinterIsNull(com.aeg.core.seal.SealStatus.DISPONIBLE))
				.thenReturn(List.of(availableSeal));

		var visible = service.findVisibleSeals();

		assertThat(visible).extracting(com.aeg.core.seal.Seal::getSerial)
				.containsExactly("S-INSTALLED", "S-AVAILABLE");
	}
}
