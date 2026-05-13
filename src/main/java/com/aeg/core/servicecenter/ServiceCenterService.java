package com.aeg.core.servicecenter;

import java.util.List;

import com.aeg.core.servicecenter.dto.ServiceCenterRequest;
import com.aeg.core.servicecenter.dto.ServiceCenterResponse;

public interface ServiceCenterService {

	List<ServiceCenterResponse> findAll();

	ServiceCenterResponse findById(Long id);

	ServiceCenterResponse create(ServiceCenterRequest request);

	ServiceCenterResponse update(Long id, ServiceCenterRequest request);

	void delete(Long id);
}