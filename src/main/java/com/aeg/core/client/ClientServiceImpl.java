package com.aeg.core.client;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.client.dto.ClientRequest;
import com.aeg.core.client.dto.ClientResponse;
import com.aeg.core.security.Role;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

	private final ClientRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;
	private final com.aeg.core.distributor.DistributorRepository distributorRepository;

	public ClientServiceImpl(
			ClientRepository repository,
			com.aeg.core.branch.BranchRepository branchRepository,
			com.aeg.core.distributor.DistributorRepository distributorRepository) {
		this.repository = repository;
		this.branchRepository = branchRepository;
		this.distributorRepository = distributorRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponse> findAll() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		if (currentUser.getRole() == Role.ADMIN) {
			return repository.findAll().stream()
				.map(this::toResponse)
				.toList();
		} else if (currentUser.getRole() == Role.DISTRIBUTOR && currentUser.getDistributorId() != null) {
			return repository.findByDistributor_Id(currentUser.getDistributorId()).stream()
				.map(this::toResponse)
				.toList();
		}
		
		return List.of();
	}

	@Override
	@Transactional(readOnly = true)
	public ClientResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Override
	public ClientResponse create(ClientRequest request) {
		Client client = new Client();
		client.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
		if (request.distributorId() != null) {
			client.setDistributor(distributorRepository.findById(request.distributorId())
					.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.distributorId())));
		}
		return toResponse(repository.save(client));
	}

	@Override
	public ClientResponse update(Long id, ClientRequest request) {
		Client client = findEntityById(id);
		client.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
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
			client.getCreatedAt()
		);
	}
}