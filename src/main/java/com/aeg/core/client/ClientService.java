package com.aeg.core.client;

import java.util.List;

import com.aeg.core.client.dto.ClientRequest;
import com.aeg.core.client.dto.ClientResponse;

public interface ClientService {

	List<ClientResponse> findAll();

	ClientResponse findById(Long id);

	ClientResponse create(ClientRequest request);

	ClientResponse update(Long id, ClientRequest request);

	void delete(Long id);
}