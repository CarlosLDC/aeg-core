package com.aeg.core.inspection;

import java.util.List;

import com.aeg.core.inspection.dto.AnnualInspectionRequest;
import com.aeg.core.inspection.dto.AnnualInspectionResponse;

public interface AnnualInspectionService {

	List<AnnualInspectionResponse> findAll();

	AnnualInspectionResponse findById(Long id);

	AnnualInspectionResponse create(AnnualInspectionRequest request);

	AnnualInspectionResponse update(Long id, AnnualInspectionRequest request);

	void delete(Long id);
}
