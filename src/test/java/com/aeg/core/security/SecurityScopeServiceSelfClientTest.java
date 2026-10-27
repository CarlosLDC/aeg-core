package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SecurityScopeServiceSelfClientTest {

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
		ClientBranchLinkPolicy clientBranchLinkPolicy = new ClientBranchLinkPolicy(
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

	@Test
	void assertCanLinkClientToBranch_allowsDistributorSelfClient() {
		Branch branch = new Branch();
		branch.setId(42L);
		branch.setOrganizationRole(com.aeg.core.branch.BranchOrganizationRole.DISTRIBUTOR);
		Distributor distributor = new Distributor();
		distributor.setId(7L);
		distributor.setBranch(branch);

		User adminUser = User.builder().role(Role.ADMIN).build();
		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(adminUser, null, java.util.List.of()));

		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.assertCanLinkClientToBranch(42L, 7L));
	}
}
