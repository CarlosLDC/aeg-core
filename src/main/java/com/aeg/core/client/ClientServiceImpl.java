package com.aeg.core.client;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.client.dto.ClientRequest;
import com.aeg.core.client.dto.ClientResponse;
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
	private final SecurityScopeService securityScope;

	public ClientServiceImpl(
			ClientRepository repository,
			com.aeg.core.branch.BranchRepository branchRepository,
			com.aeg.core.distributor.DistributorRepository distributorRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.branchRepository = branchRepository;
		this.distributorRepository = distributorRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponse> findAll() {
		User currentUser = securityScope.currentUser();
		if (currentUser.getRole() == Role.ADMIN) {
			return repository.findAll().stream().map(this::toResponse).toList();
		}
		if (currentUser.getRole() == Role.DISTRIBUTOR && currentUser.getDistributorId() != null) {
			return repository.findByDistributor_Id(currentUser.getDistributorId()).stream()
					.map(this::toResponse)
					.toList();
		}
		BranchScope scope = securityScope.resolveBranchScope();
		if (scope.visibility() != BranchScope.Visibility.SCOPED) {
			return List.of();
		}
		return repository.findByBranch_IdIn(scope.branchIds()).stream().map(this::toResponse).toList();
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
				user.getRole() == Role.DISTRIBUTOR ? user.getDistributorId() : null;
		securityScope.assertCanLinkClientToBranch(branchId, distributorId);
		return repository.findFirstByBranch_Id(branchId).map(this::toResponse);
	}

	@Override
	public ClientResponse create(ClientRequest request) {
		Optional<Client> existing = repository.findFirstByBranch_Id(request.branchId());
		if (existing.isPresent()) {
			return linkOrUpdateExistingClient(existing.get(), request);
		}
		return createNewClient(request);
	}

	private ClientResponse createNewClient(ClientRequest request) {
		Client client = new Client();
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertCanLinkClientToBranch(branch.getId(), request.distributorId());
		client.setBranch(branch);
		applyDistributor(client, request.distributorId());
		return toResponse(repository.save(client));
	}

	private ClientResponse linkOrUpdateExistingClient(Client client, ClientRequest request) {
		securityScope.assertCanLinkClientToBranch(client.getBranchId(), request.distributorId());
		Long existingDistributorId = client.getDistributorId();
		if (request.distributorId() != null) {
			if (existingDistributorId != null && !existingDistributorId.equals(request.distributorId())) {
				throw new IllegalArgumentException(
						"branch already linked to another distributor");
			}
			if (existingDistributorId == null) {
				applyDistributor(client, request.distributorId());
				client = repository.save(client);
			}
		}
		return toResponse(client);
	}

	private void applyDistributor(Client client, Long distributorId) {
		if (distributorId == null) {
			client.setDistributor(null);
			return;
		}
		client.setDistributor(distributorRepository.findById(distributorId)
				.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + distributorId)));
	}

	@Override
	public ClientResponse update(Long id, ClientRequest request) {
		Client client = findEntityById(id);
		securityScope.assertClientInScope(client);
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertCanLinkClientToBranch(branch.getId(), request.distributorId());
		client.setBranch(branch);
		if (request.distributorId() != null) {
			client.setDistributor(distributorRepository.findById(request.distributorId())
					.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.distributorId())));
		} else {
			client.setDistributor(null);
		}
		return toResponse(repository.save(client));
	}

	@Override
	public void delete(Long id) {
		Client client = findEntityById(id);
		securityScope.assertClientInScope(client);
		repository.delete(client);
	}

	private Client findEntityById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
	}

	private ClientResponse toResponse(Client client) {
		return new ClientResponse(
				client.getId(),
				client.getBranchId(),
				client.getDistributorId(),
				client.getCreatedAt());
	}
}
