package com.aeg.core.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.servicecenter.ServiceCenterRepository;

@ExtendWith(MockitoExtension.class)
class ClientBranchLinkPolicyTest {

	@Mock
	private BranchRepository branchRepository;

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private DistributorRepository distributorRepository;

	@Mock
	private ServiceCenterRepository serviceCenterRepository;

	private ClientBranchLinkPolicy policy;

	@BeforeEach
	void setUp() {
		policy = new ClientBranchLinkPolicy(
				branchRepository,
				clientRepository,
				distributorRepository,
				serviceCenterRepository);
	}

	@Test
	void rejectsBranchThatIsServiceCenter() {
		Branch branch = new Branch();
		branch.setId(10L);
		branch.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);
		when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
		when(distributorRepository.findByBranch_Id(10L)).thenReturn(Optional.empty());
		when(serviceCenterRepository.findByBranch_Id(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> policy.assertFieldUserMayLinkClient(10L, 5L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(ClientBranchLinkPolicy.REASSIGNMENT_REQUIRED_MESSAGE);
	}

	@Test
	void rejectsBranchLinkedToAnotherDistributor() {
		Branch branch = new Branch();
		branch.setId(10L);
		branch.setOrganizationRole(BranchOrganizationRole.NONE);
		Client client = new Client();
		client.setDistributorId(99L);
		client.setBranch(branch);

		when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
		when(distributorRepository.findByBranch_Id(10L)).thenReturn(Optional.empty());
		when(serviceCenterRepository.findByBranch_Id(10L)).thenReturn(Optional.empty());
		when(clientRepository.findByBranch_Id(10L)).thenReturn(Optional.of(client));

		assertThatThrownBy(() -> policy.assertFieldUserMayLinkClient(10L, 5L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(ClientBranchLinkPolicy.REASSIGNMENT_REQUIRED_MESSAGE);
	}
}
