package com.aeg.core.distributorperson;

import java.util.List;

import com.aeg.core.distributorperson.dto.DistributorPersonRequest;
import com.aeg.core.distributorperson.dto.DistributorPersonResponse;

public interface DistributorPersonService {

	List<DistributorPersonResponse> findAll();

	DistributorPersonResponse findById(Long id);

	DistributorPersonResponse create(DistributorPersonRequest request);

	DistributorPersonResponse update(Long id, DistributorPersonRequest request);

	void delete(Long id);
}
