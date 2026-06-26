package com.aeg.core.modificationrequest.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.client.ClientReviewStatus;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequest;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.modificationrequest.client.dto.ClientModificationProposedData;
import com.aeg.core.modificationrequest.client.dto.ClientModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.client.dto.ClientModificationRequestListItemResponse;
import com.aeg.core.modificationrequest.client.dto.ClientSnapshotResponse;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class ClientModificationRequestServiceImpl implements ClientModificationRequestService {

	private final ModificationRequestRepository repository;
	private final ClientRepository clientRepository;
	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final com.aeg.core.distributor.DistributorRepository distributorRepository;
	private final com.aeg.core.printer.PrinterRepository printerRepository;
	private final SecurityScopeService securityScope;
	private final ObjectMapper objectMapper;

	public ClientModificationRequestServiceImpl(
			ModificationRequestRepository repository,
			ClientRepository clientRepository,
			CompanyRepository companyRepository,
			BranchRepository branchRepository,
			com.aeg.core.distributor.DistributorRepository distributorRepository,
			com.aeg.core.printer.PrinterRepository printerRepository,
			SecurityScopeService securityScope,
			ObjectMapper objectMapper) {
		this.repository = repository;
		this.clientRepository = clientRepository;
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.distributorRepository = distributorRepository;
		this.printerRepository = printerRepository;
		this.securityScope = securityScope;
		this.objectMapper = objectMapper;
	}

	@Override
	public ClientModificationRequestDetailResponse requestUpdate(
			Long clientId,
			ClientModificationProposedData proposedData) {
		Client client = findClient(clientId);
		securityScope.assertClientInScope(client);
		assertClientNotPending(client);

		User requestedBy = securityScope.currentUser();
		client.setReviewStatus(ClientReviewStatus.PENDING_REVIEW);
		clientRepository.save(client);

		ModificationRequest request = new ModificationRequest();
		request.setTargetType(ModificationTargetType.CLIENT);
		request.setTargetId(client.getId());
		request.setActionType(ModificationActionType.UPDATE);
		request.setProposedData(toProposedDataMap(proposedData));
		request.setRequestedBy(requestedBy);
		request.setStatus(ModificationRequestStatus.PENDING);

		return toDetailResponse(repository.save(request), client);
	}

	@Override
	public ClientModificationRequestDetailResponse requestDelete(Long clientId) {
		Client client = findClient(clientId);
		securityScope.assertClientInScope(client);
		assertClientNotPending(client);

		User requestedBy = securityScope.currentUser();
		client.setReviewStatus(ClientReviewStatus.PENDING_REVIEW);
		clientRepository.save(client);

		ModificationRequest request = new ModificationRequest();
		request.setTargetType(ModificationTargetType.CLIENT);
		request.setTargetId(client.getId());
		request.setActionType(ModificationActionType.DELETE);
		request.setRequestedBy(requestedBy);
		request.setStatus(ModificationRequestStatus.PENDING);

		return toDetailResponse(repository.save(request), client);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientModificationRequestListItemResponse> findByStatus(ModificationRequestStatus status) {
		ModificationRequestStatus target = status == null ? ModificationRequestStatus.PENDING : status;
		return repository.findByTargetTypeAndStatusOrderByCreatedAtDesc(
				ModificationTargetType.CLIENT,
				target).stream()
				.map(this::toListItemResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ClientModificationRequestDetailResponse findById(Long id) {
		ModificationRequest request = findRequest(id);
		if (request.getTargetType() != ModificationTargetType.CLIENT) {
			throw new ResourceNotFoundException("Modification request not found with id: " + id);
		}
		Client client = clientRepository.findById(request.getTargetId()).orElse(null);
		return toDetailResponse(request, client);
	}

	@Override
	public ClientModificationRequestDetailResponse approve(Long id) {
		ModificationRequest request = findPendingRequest(id);
		if (request.getTargetType() != ModificationTargetType.CLIENT) {
			throw new ResourceNotFoundException("Modification request not found with id: " + id);
		}
		Client client = findClient(request.getTargetId());
		assertClientPending(client);

		if (request.getActionType() == ModificationActionType.UPDATE) {
			ClientModificationProposedData proposed = toProposedData(request.getProposedData());
			applyUpdate(client, proposed);
			client.setReviewStatus(ClientReviewStatus.ACTIVE);
			clientRepository.save(client);
		} else {
			if (printerRepository.existsByClient_Id(client.getId())) {
				throw new IllegalArgumentException("client has printers and cannot be deleted: " + client.getId());
			}
			clientRepository.delete(client);
		}

		request.setStatus(ModificationRequestStatus.APPROVED);
		ModificationRequest saved = repository.save(request);
		Client current = clientRepository.findById(request.getTargetId()).orElse(null);
		return toDetailResponse(saved, current);
	}

	@Override
	public ClientModificationRequestDetailResponse reject(Long id) {
		ModificationRequest request = findPendingRequest(id);
		if (request.getTargetType() != ModificationTargetType.CLIENT) {
			throw new ResourceNotFoundException("Modification request not found with id: " + id);
		}
		Client client = findClient(request.getTargetId());
		assertClientPending(client);

		client.setReviewStatus(ClientReviewStatus.ACTIVE);
		clientRepository.save(client);

		request.setStatus(ModificationRequestStatus.REJECTED);
		return toDetailResponse(repository.save(request), client);
	}

	@Override
	public ClientModificationRequestDetailResponse cancel(Long id) {
		ModificationRequest request = findPendingRequest(id);
		if (request.getTargetType() != ModificationTargetType.CLIENT) {
			throw new ResourceNotFoundException("Modification request not found with id: " + id);
		}
		assertRequesterCanCancel(request);
		Client client = findClient(request.getTargetId());
		securityScope.assertClientInScope(client);
		assertClientPending(client);

		client.setReviewStatus(ClientReviewStatus.ACTIVE);
		clientRepository.save(client);

		request.setStatus(ModificationRequestStatus.REJECTED);
		return toDetailResponse(repository.save(request), client);
	}

	private void assertRequesterCanCancel(ModificationRequest request) {
		User user = securityScope.currentUser();
		if (!Role.isDistributorScoped(user.getRole())) {
			throw new AccessDeniedException("Only distributors can cancel modification requests");
		}
		if (!request.getRequestedBy().getId().equals(user.getId())) {
			throw new AccessDeniedException("Only the requester can cancel this modification request");
		}
	}

	private void applyUpdate(Client client, ClientModificationProposedData proposed) {
		Branch branch = branchRepository.findById(client.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + client.getBranchId()));
		Company company = branch.getCompany();
		if (company == null) {
			throw new ResourceNotFoundException("Company not found for branch id: " + branch.getId());
		}

		String rif = proposed.rif() == null ? "" : proposed.rif().trim();
		if (!rif.equalsIgnoreCase(company.getRif()) && companyRepository.existsByRif(rif)) {
			throw new IllegalArgumentException("rif already exists: " + rif);
		}

		company.setBusinessName(proposed.businessName());
		company.setRif(rif);
		company.setContributorType(proposed.contributorType());
		companyRepository.save(company);

		branch.setCity(proposed.city());
		branch.setState(proposed.state());
		branch.setAddress(proposed.address());
		branch.setContactPersonName(proposed.contactPersonName());
		branch.setPhone(proposed.phone());
		branch.setEmail(proposed.email());
		branchRepository.save(branch);

		Long distributorId = proposed.distributorId();
		if (distributorId == null) {
			client.setDistributor(null);
			client.setDistributorId(null);
			return;
		}
		var distributor = distributorRepository.findById(distributorId)
				.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + distributorId));
		client.setDistributor(distributor);
		client.setDistributorId(distributorId);
	}

	private ModificationRequest findRequest(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Modification request not found with id: " + id));
	}

	private ModificationRequest findPendingRequest(Long id) {
		ModificationRequest request = findRequest(id);
		if (request.getStatus() != ModificationRequestStatus.PENDING) {
			throw new IllegalArgumentException("modification request is no longer pending");
		}
		return request;
	}

	private Client findClient(Long id) {
		return clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
	}

	private void assertClientNotPending(Client client) {
		if (client.getReviewStatus() == ClientReviewStatus.PENDING_REVIEW) {
			throw new IllegalArgumentException("client has a pending review request");
		}
	}

	private void assertClientPending(Client client) {
		if (client.getReviewStatus() != ClientReviewStatus.PENDING_REVIEW) {
			throw new IllegalArgumentException("client is not in pending review state");
		}
	}

	private Map<String, Object> toProposedDataMap(ClientModificationProposedData proposed) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("businessName", proposed.businessName());
		map.put("rif", proposed.rif());
		map.put("contributorType", proposed.contributorType() == null ? null : proposed.contributorType().getValue());
		map.put("city", proposed.city());
		map.put("state", proposed.state());
		map.put("address", proposed.address());
		map.put("contactPersonName", proposed.contactPersonName());
		map.put("phone", proposed.phone());
		map.put("email", proposed.email());
		map.put("distributorId", proposed.distributorId());
		return map;
	}

	private ClientModificationProposedData toProposedData(Map<String, Object> proposedData) {
		if (proposedData == null || proposedData.isEmpty()) {
			throw new IllegalArgumentException("proposedData is required for UPDATE requests");
		}
		return objectMapper.convertValue(proposedData, ClientModificationProposedData.class);
	}

	private ClientModificationRequestListItemResponse toListItemResponse(ModificationRequest request) {
		Client client = clientRepository.findById(request.getTargetId()).orElse(null);
		String clientName = "Cliente eliminado";
		if (client != null && client.getBranch() != null && client.getBranch().getCompany() != null) {
			clientName = client.getBranch().getCompany().getBusinessName();
		}
		return new ClientModificationRequestListItemResponse(
				request.getId(),
				request.getTargetId(),
				clientName,
				request.getActionType(),
				request.getStatus(),
				request.getRequestedBy().getId(),
				request.getRequestedBy().getName(),
				request.getCreatedAt());
	}

	private ClientModificationRequestDetailResponse toDetailResponse(ModificationRequest request, Client client) {
		return new ClientModificationRequestDetailResponse(
				request.getId(),
				request.getTargetId(),
				request.getActionType(),
				request.getStatus(),
				request.getProposedData(),
				toSnapshot(client),
				request.getRequestedBy().getId(),
				request.getRequestedBy().getName(),
				request.getCreatedAt());
	}

	private ClientSnapshotResponse toSnapshot(Client client) {
		if (client == null) {
			return null;
		}
		Branch branch = client.getBranch();
		Company company = branch != null ? branch.getCompany() : null;
		return new ClientSnapshotResponse(
				client.getId(),
				branch != null ? branch.getId() : null,
				client.getDistributorId(),
				client.getReviewStatus(),
				company != null ? company.getId() : null,
				company != null ? company.getBusinessName() : null,
				company != null ? company.getRif() : null,
				company != null ? company.getContributorType() : null,
				branch != null ? branch.getCity() : null,
				branch != null ? branch.getState() : null,
				branch != null ? branch.getAddress() : null,
				branch != null ? branch.getContactPersonName() : null,
				branch != null ? branch.getPhone() : null,
				branch != null ? branch.getEmail() : null);
	}
}
