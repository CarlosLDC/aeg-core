package com.aeg.core.distribuidor;

import java.util.List;

import com.aeg.core.distribuidor.dto.DistribuidorRequest;
import com.aeg.core.distribuidor.dto.DistribuidorResponse;

public interface DistribuidorService {

	List<DistribuidorResponse> findAll();

	DistribuidorResponse findById(Long id);

	DistribuidorResponse create(DistribuidorRequest request);

	DistribuidorResponse update(Long id, DistribuidorRequest request);

	void delete(Long id);
}
