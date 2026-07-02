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
		service = new SecurityScopeService(
				branchRepository,
				clientRepository,
				distributorRepository,
				printerRepository,
				sealRepository,
				serviceCenterRepository);
	}

	@Test
	void assertCanLinkClientToBranch_rejectsDistributorSelfClient() {
		Branch branch = new Branch();
		branch.setId(42L);
		Distributor distributor = new Distributor();
		distributor.setId(7L);
		distributor.setBranch(branch);

		when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));

		assertThatThrownBy(() -> service.assertCanLinkClientToBranch(42L, 7L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("cannot be client of itself");
	}
}
