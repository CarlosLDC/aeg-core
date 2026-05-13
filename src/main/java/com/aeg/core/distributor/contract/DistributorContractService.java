package com.aeg.core.distributor.contract;

import java.util.List;

import com.aeg.core.distributor.contract.dto.DistributorContractRequest;
import com.aeg.core.distributor.contract.dto.DistributorContractResponse;

public interface DistributorContractService {

	List<DistributorContractResponse> findAll();

	DistributorContractResponse findById(Long id);

	DistributorContractResponse create(DistributorContractRequest request);

	DistributorContractResponse update(Long id, DistributorContractRequest request);

	void delete(Long id);
}
