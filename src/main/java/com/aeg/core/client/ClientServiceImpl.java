package com.aeg.core.client;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.client.dto.ClientRequest;
import com.aeg.core.client.dto.ClientResponse;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

	private final ClientRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;
	private final com.aeg.core.distributor.DistributorRepository distributorRepository;
	private final ModificationRequestRepository modificationRequestRepository;
	private final SecurityScopeService securityScope;

	public ClientServiceImpl(
			ClientRepository repository,
			com.aeg.core.branch.BranchRepository branchRepository,
			com.aeg.core.distributor.DistributorRepository distributorRepository,
			ModificationRequestRepository modificationRequestRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.branchRepository = branchRepository;
		this.distributorRepository = distributorRepository;
		this.modificationRequestRepository = modificationRequestRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponse> findAll() {
		User currentUser = securityScope.currentUser();
		if (currentUser.getRole() == Role.ADMIN) {
			return repository.findAllFetched().stream().map(this::toResponse).toList();
		}
		if (Role.isDistributorScoped(currentUser.getRole()) && currentUser.getDistributorId() != null) {
			Long distributorId = currentUser.getDistributorId();
			return repository.findAllFetchedByDistributorId(distributorId).stream()
					.filter(client -> !isDistributorSelfClient(client, distributorId))
					.map(this::toResponse)
					.toList();
		}
		BranchScope scope = securityScope.resolveBranchScope();
		if (scope.visibility() != BranchScope.Visibility.SCOPED) {
			return List.of();
		}
		return repository.findAllFetchedByBranch_IdIn(scope.branchIds()).stream()
				.filter(client -> !isDistributorSelfClient(client, currentUser.getDistributorId()))
				.map(this::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ClientResponse findById(Long id) {
		Client client = findEntityById(id);
		securityScope.assertClientInScope(client);
		return toResponse(client);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ClientResponse> findByBranchId(Long branchId) {
		User user = securityScope.currentUser();
		Long distributorId =
				Role.isDistributorScoped(user.getRole()) ? user.getDistributorId() : null;
		securityScope.assertCanLinkClientToBranch(branchId, distributorId);
		Client client = resolveClientForBranch(branchId);
		return client == null ? Optional.empty() : Optional.of(toResponse(client));
	}

	@Override
	public ClientResponse create(ClientRequest request) {
		securityScope.assertCanLinkClientToBranch(request.branchId(), request.distributorId());
		Client existing = resolveClientForBranch(request.branchId());
		if (existing != null) {
			Long linked = existing.getDistributorId();
			if (linked != null && linked.equals(request.distributorId())) {
				markBranchAsClient(request.branchId());
				return toResponseForBranch(request.branchId());
			}
			return linkOrUpdateExistingClient(existing, request);
		}
		return createNewClient(request);
	}

	private Client resolveClientForBranch(Long branchId) {
		var rows = repository.findAllFetchedByBranchId(branchId);
		if (rows.isEmpty()) {
			return null;
		}
		for (Client row : rows) {
			if (row.getBranch() == null) {
				repository.delete(row);
			}
		}
		var valid = repository.findAllFetchedByBranchId(branchId);
		if (valid.isEmpty()) {
			return null;
		}
		Client primary = valid.get(0);
		for (int i = 1; i < valid.size(); i++) {
			repository.delete(valid.get(i));
		}
		return primary;
	}

	private Branch requireBranchWithCompany(Long branchId) {
		return branchRepository
				.findByIdWithCompany(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
	}

	private ClientResponse createNewClient(ClientRequest request) {
		Branch branch = requireBranchWithCompany(request.branchId());
		Client client = new Client();
		client.setBranch(branch);
		applyDistributor(client, request.distributorId());
		repository.saveAndFlush(client);
		markBranchAsClient(branch.getId());
		return toResponseForBranch(request.branchId());
	}

	private ClientResponse linkOrUpdateExistingClient(Client client, ClientRequest request) {
		Long branchId = client.getBranchId();
		if (branchId == null) {
			throw new IllegalArgumentException("Client record is missing branch reference");
		}
		securityScope.assertCanLinkClientToBranch(branchId, request.distributorId());
		Long existingDistributorId = client.getDistributorId();
		if (request.distributorId() != null) {
			if (existingDistributorId != null && !existingDistributorId.equals(request.distributorId())) {
				throw new IllegalArgumentException(
						"branch already linked to another distributor");
			}
			if (existingDistributorId == null) {
				Client managed = repository.findAllFetchedByBranchId(branchId).stream()
						.findFirst()
						.orElse(client);
				applyDistributor(managed, request.distributorId());
				if (managed.getBranch() == null) {
					managed.setBranch(requireBranchWithCompany(branchId));
				}
				repository.saveAndFlush(managed);
			}
		}
		markBranchAsClient(branchId);
		return toResponseForBranch(branchId);
	}

	private void markBranchAsClient(Long branchId) {
		branchRepository.markAsClient(branchId);
	}

	private ClientResponse toResponseForBranch(Long branchId) {
		Client client = resolveClientForBranch(branchId);
		if (client == null) {
			throw new IllegalStateException("Client link missing for branch id: " + branchId);
		}
		return toResponse(client);
	}

	private void applyDistributor(Client client, Long distributorId) {
		if (distributorId == null) {
			client.setDistributorId(null);
			client.setDistributor(null);
			return;
		}
		var distributor = distributorRepository.findById(distributorId)
				.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + distributorId));
		client.setDistributor(distributor);
		client.setDistributorId(distributorId);
	}

	@Override
	public ClientResponse update(Long id, ClientRequest request) {
		Client client = findEntityById(id);
		assertClientNotPending(client);
		User user = securityScope.currentUser();
		if (Role.isDistributorScoped(user.getRole()) && client.getDistributorId() == null) {
			securityScope.assertCanLinkClientToBranch(request.branchId(), request.distributorId());
		} else {
			securityScope.assertClientInScope(client);
			securityScope.assertCanLinkClientToBranch(request.branchId(), request.distributorId());
		}
		var branch = requireBranchWithCompany(request.branchId());
		client.setBranch(branch);
		if (request.distributorId() != null) {
			client.setDistributor(distributorRepository.findById(request.distributorId())
					.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.distributorId())));
		} else {
			client.setDistributor(null);
		}
		markBranchAsClient(branch.getId());
		return toResponse(repository.save(client));
	}

	@Override
	public ClientResponse transferDistributor(Long clientId, Long targetDistributorId) {
		User user = securityScope.currentUser();
		if (user.getRole() != Role.ADMIN) {
			throw new IllegalArgumentException("only administrators can transfer client distributors");
		}
		Client client = findEntityById(clientId);
		assertClientNotPending(client);
		Long currentDistributorId = client.getDistributorId();
		if (currentDistributorId != null && currentDistributorId.equals(targetDistributorId)) {
			throw new IllegalArgumentException("client is already assigned to this distributor");
		}
		applyDistributor(client, targetDistributorId);
		return toResponse(repository.save(client));
	}

	@Override
	public void delete(Long id) {
		Client client = findEntityById(id);
		securityScope.assertClientInScope(client);
		assertClientNotPending(client);
		repository.delete(client);
	}

	private Client findEntityById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
	}

	private ClientResponse toResponse(Client client) {
		var branch = client.getBranch();
		var company = branch != null ? branch.getCompany() : null;
		Long distributorId = client.getDistributorId();
		Long activeRequestId = modificationRequestRepository
				.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
						ModificationTargetType.CLIENT,
						client.getId(),
						ModificationRequestStatus.PENDING)
				.map(mr -> mr.getId())
				.orElse(null);
		return new ClientResponse(
				client.getId(),
				branch != null ? branch.getId() : null,
				distributorId,
				client.getCreatedAt(),
				client.getReviewStatus(),
				activeRequestId,
				branch != null ? branch.getCity() : null,
				branch != null ? branch.getState() : null,
				company != null ? company.getBusinessName() : null,
				company != null ? company.getRif() : null,
				branch != null ? branch.getPhone() : null,
				branch != null ? branch.getEmail() : null);
	}

	private void assertClientNotPending(Client client) {
		if (client.getReviewStatus() == ClientReviewStatus.PENDING_REVIEW) {
			throw new IllegalArgumentException("client has a pending review request");
		}
	}

	private boolean isDistributorSelfClient(Client client, Long distributorId) {
		if (distributorId == null || client.getBranchId() == null) {
			return false;
		}
		return distributorRepository.findById(distributorId)
				.map(distributor -> client.getBranchId().equals(distributor.getBranchId()))
				.orElse(false);
	}
}
