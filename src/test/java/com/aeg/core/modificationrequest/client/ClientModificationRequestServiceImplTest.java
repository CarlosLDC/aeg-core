package com.aeg.core.modificationrequest.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.client.ClientReviewStatus;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.company.ContributorType;
import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequest;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.modificationrequest.client.dto.ClientModificationProposedData;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.fasterxml.jackson.databind.ObjectMapper;

class ClientModificationRequestServiceImplTest {

	private ModificationRequestRepository modificationRequestRepository;
	private ClientRepository clientRepository;
	private CompanyRepository companyRepository;
	private BranchRepository branchRepository;
	private com.aeg.core.distributor.DistributorRepository distributorRepository;
	private com.aeg.core.printer.PrinterRepository printerRepository;
	private SecurityScopeService securityScope;
	private ClientModificationRequestServiceImpl service;

	@BeforeEach
	void setup() {
		modificationRequestRepository = mock(ModificationRequestRepository.class);
		clientRepository = mock(ClientRepository.class);
		companyRepository = mock(CompanyRepository.class);
		branchRepository = mock(BranchRepository.class);
		distributorRepository = mock(com.aeg.core.distributor.DistributorRepository.class);
		printerRepository = mock(com.aeg.core.printer.PrinterRepository.class);
		securityScope = mock(SecurityScopeService.class);

		service = new ClientModificationRequestServiceImpl(
				modificationRequestRepository,
				clientRepository,
				companyRepository,
				branchRepository,
				distributorRepository,
				printerRepository,
				securityScope,
				new ObjectMapper());
	}

	@Test
	void requestUpdate_createsPendingClientRequestWithProposedData() {
		Client client = client(25L);
		when(clientRepository.findById(25L)).thenReturn(Optional.of(client));

		User requester = new User();
		requester.setId(77L);
		requester.setName("Distribuidor");
		requester.setRole(Role.DISTRIBUTOR);
		when(securityScope.currentUser()).thenReturn(requester);
		when(modificationRequestRepository.save(any(ModificationRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ClientModificationProposedData proposed = new ClientModificationProposedData(
				"Cliente Test",
				"J123456789",
				ContributorType.ORDINARIO,
				"Caracas",
				"Distrito Capital",
				"Av. Principal",
				"Ana Pérez",
				"02121234567",
				"cliente@test.com",
				15L);

		var response = service.requestUpdate(25L, proposed);

		assertThat(response.status()).isEqualTo(ModificationRequestStatus.PENDING);
		assertThat(client.getReviewStatus()).isEqualTo(ClientReviewStatus.PENDING_REVIEW);
		ArgumentCaptor<ModificationRequest> captor = ArgumentCaptor.forClass(ModificationRequest.class);
		verify(modificationRequestRepository).save(captor.capture());
		ModificationRequest stored = captor.getValue();
		assertThat(stored.getTargetType()).isEqualTo(ModificationTargetType.CLIENT);
		assertThat(stored.getTargetId()).isEqualTo(25L);
		assertThat(stored.getActionType()).isEqualTo(ModificationActionType.UPDATE);
		Map<String, Object> data = stored.getProposedData();
		assertThat(data.get("rif")).isEqualTo("J123456789");
		assertThat(data.get("city")).isEqualTo("Caracas");
	}

	@Test
	void approveDelete_withLinkedPrinters_throwsConflict() {
		Client client = client(31L);
		client.setReviewStatus(ClientReviewStatus.PENDING_REVIEW);

		ModificationRequest request = new ModificationRequest();
		request.setId(100L);
		request.setTargetType(ModificationTargetType.CLIENT);
		request.setTargetId(31L);
		request.setActionType(ModificationActionType.DELETE);
		request.setStatus(ModificationRequestStatus.PENDING);
		User requester = new User();
		requester.setId(1L);
		requester.setName("Admin");
		request.setRequestedBy(requester);

		when(modificationRequestRepository.findById(100L)).thenReturn(Optional.of(request));
		when(clientRepository.findById(31L)).thenReturn(Optional.of(client));
		when(printerRepository.existsByClient_Id(31L)).thenReturn(true);

		assertThatThrownBy(() -> service.approve(100L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("cannot be deleted");
	}

	private static Client client(Long id) {
		Company company = new Company();
		company.setId(9L);
		company.setBusinessName("Cliente");
		company.setRif("J000000000");
		company.setContributorType(ContributorType.ORDINARIO);

		Branch branch = new Branch();
		branch.setId(7L);
		branch.setCompany(company);
		branch.setCity("Caracas");
		branch.setState("Distrito Capital");

		Client client = new Client();
		client.setId(id);
		client.setBranch(branch);
		client.setDistributorId(15L);
		client.setReviewStatus(ClientReviewStatus.ACTIVE);
		return client;
	}
}
