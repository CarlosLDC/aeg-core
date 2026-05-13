package com.aeg.core.client;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.client.dto.ClientRequest;
import com.aeg.core.client.dto.ClientResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

	private final ClientRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;

	public ClientServiceImpl(ClientRepository repository, com.aeg.core.branch.BranchRepository branchRepository) {
		this.repository = repository;
		this.branchRepository = branchRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponse> findAll() {
		return repository.findAll().stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ClientResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Override
	public ClientResponse create(ClientRequest request) {
		Client client = new Client();
		client.setBranch(branchRepository.getReferenceById(request.branchId()));
		return toResponse(repository.save(client));
	}

	@Override
	public ClientResponse update(Long id, ClientRequest request) {
		Client client = findEntityById(id);
		client.setBranch(branchRepository.getReferenceById(request.branchId()));
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
			client.getCreatedAt()
		);
	}
}