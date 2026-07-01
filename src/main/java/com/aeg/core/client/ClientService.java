package com.aeg.core.client;

import java.util.List;

import java.util.Optional;

import com.aeg.core.client.dto.ClientRequest;
import com.aeg.core.client.dto.ClientResponse;

public interface ClientService {

	List<ClientResponse> findAll();

	ClientResponse findById(Long id);

	Optional<ClientResponse> findByBranchId(Long branchId);

	ClientResponse create(ClientRequest request);

	ClientResponse update(Long id, ClientRequest request);

	ClientResponse transferDistributor(Long clientId, Long targetDistributorId);

	void delete(Long id);
}