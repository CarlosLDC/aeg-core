package com.aeg.core.technicalservice;

import java.util.List;

import com.aeg.core.technicalservice.dto.TechnicalServiceRequest;
import com.aeg.core.technicalservice.dto.TechnicalServiceResponse;

public interface TechnicalServiceService {

	List<TechnicalServiceResponse> findAll();

	TechnicalServiceResponse findById(Long id);

	TechnicalServiceResponse create(TechnicalServiceRequest request);

	TechnicalServiceResponse update(Long id, TechnicalServiceRequest request);

	void delete(Long id);
}
