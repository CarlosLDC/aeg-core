package com.aeg.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.ContributorType;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;

class ClientServiceImplTransferTest {

	private ClientRepository clientRepository;
	private BranchRepository branchRepository;
	private DistributorRepository distributorRepository;
	private ModificationRequestRepository modificationRequestRepository;
	private SecurityScopeService securityScope;
	private ClientServiceImpl service;

	@BeforeEach
	void setup() {
		clientRepository = mock(ClientRepository.class);
		branchRepository = mock(BranchRepository.class);
		distributorRepository = mock(DistributorRepository.class);
		modificationRequestRepository = mock(ModificationRequestRepository.class);
		securityScope = mock(SecurityScopeService.class);

		service = new ClientServiceImpl(
				clientRepository,
				branchRepository,
				distributorRepository,
				modificationRequestRepository,
				securityScope);
	}

	@Test
	void transferDistributor_admin_updatesClientDistributor() {
		Client client = activeClient(10L, 5L);
		Distributor target = distributor(8L);

		when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
		when(securityScope.currentUser()).thenReturn(adminUser());
		when(distributorRepository.findById(8L)).thenReturn(Optional.of(target));
		when(clientRepository.save(client)).thenReturn(client);
		when(modificationRequestRepository
				.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(any(), any(), any()))
				.thenReturn(Optional.empty());

		var response = service.transferDistributor(10L, 8L);

		assertThat(response.distributorId()).isEqualTo(8L);
		ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
		verify(clientRepository).save(captor.capture());
		assertThat(captor.getValue().getDistributorId()).isEqualTo(8L);
	}

	@Test
	void transferDistributor_pendingReview_throws() {
		Client client = activeClient(10L, 5L);
		client.setReviewStatus(ClientReviewStatus.PENDING_REVIEW);
		when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
		when(securityScope.currentUser()).thenReturn(adminUser());

		assertThatThrownBy(() -> service.transferDistributor(10L, 8L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pending review");
	}

	@Test
	void transferDistributor_sameDistributor_throws() {
		Client client = activeClient(10L, 5L);
		when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
		when(securityScope.currentUser()).thenReturn(adminUser());

		assertThatThrownBy(() -> service.transferDistributor(10L, 5L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already assigned");
	}

	@Test
	void transferDistributor_nonAdmin_throws() {
		Client client = activeClient(10L, 5L);
		when(clientRepository.findById(10L)).thenReturn(Optional.of(client));

		User distributorUser = new User();
		distributorUser.setRole(Role.DISTRIBUTOR);
		when(securityScope.currentUser()).thenReturn(distributorUser);

		assertThatThrownBy(() -> service.transferDistributor(10L, 8L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("only administrators");
	}

	private static User adminUser() {
		User user = new User();
		user.setRole(Role.ADMIN);
		return user;
	}

	private static Distributor distributor(Long id) {
		Distributor distributor = new Distributor();
		distributor.setId(id);
		return distributor;
	}

	private static Client activeClient(Long clientId, Long distributorId) {
		Company company = new Company();
		company.setId(1L);
		company.setBusinessName("Cliente Test");
		company.setRif("J123456789");
		company.setContributorType(ContributorType.ORDINARIO);

		Branch branch = new Branch();
		branch.setId(7L);
		branch.setCompany(company);
		branch.setCity("Caracas");
		branch.setState("Distrito Capital");

		Client client = new Client();
		client.setId(clientId);
		client.setBranch(branch);
		client.setDistributorId(distributorId);
		client.setReviewStatus(ClientReviewStatus.ACTIVE);
		return client;
	}
}
