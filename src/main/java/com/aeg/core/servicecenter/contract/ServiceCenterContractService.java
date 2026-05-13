package com.aeg.core.servicecenter.contract;

import java.util.List;

import com.aeg.core.servicecenter.contract.dto.ServiceCenterContractRequest;
import com.aeg.core.servicecenter.contract.dto.ServiceCenterContractResponse;

public interface ServiceCenterContractService {

	List<ServiceCenterContractResponse> findAll();

	ServiceCenterContractResponse findById(Long id);

	ServiceCenterContractResponse create(ServiceCenterContractRequest request);

	ServiceCenterContractResponse update(Long id, ServiceCenterContractRequest request);

	void delete(Long id);
}
