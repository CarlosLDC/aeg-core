package com.aeg.core.distributor;

import java.util.List;

import com.aeg.core.distributor.dto.DistributorRequest;
import com.aeg.core.distributor.dto.DistributorResponse;

public interface DistributorService {

	List<DistributorResponse> findAll();

	DistributorResponse findById(Long id);

	DistributorResponse create(DistributorRequest request);

	DistributorResponse update(Long id, DistributorRequest request);

	void delete(Long id);
}