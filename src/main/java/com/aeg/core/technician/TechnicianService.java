package com.aeg.core.technician;

import java.util.List;

import com.aeg.core.technician.dto.TechnicianRequest;
import com.aeg.core.technician.dto.TechnicianResponse;

public interface TechnicianService {

	List<TechnicianResponse> findAll();

	TechnicianResponse findById(Long id);

	TechnicianResponse create(TechnicianRequest request);

	TechnicianResponse update(Long id, TechnicianRequest request);

	void delete(Long id);
}
