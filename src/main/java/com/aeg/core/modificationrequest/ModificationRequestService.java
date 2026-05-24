package com.aeg.core.modificationrequest;

import java.util.List;

import com.aeg.core.employee.dto.EmployeeRequest;
import com.aeg.core.modificationrequest.dto.ModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.dto.ModificationRequestListItemResponse;

public interface ModificationRequestService {

	ModificationRequestDetailResponse requestUpdate(Long employeeId, EmployeeRequest proposedData);

	ModificationRequestDetailResponse requestDelete(Long employeeId);

	List<ModificationRequestListItemResponse> findByStatus(ModificationRequestStatus status);

	ModificationRequestDetailResponse findById(Long id);

	ModificationRequestDetailResponse approve(Long id);

	ModificationRequestDetailResponse reject(Long id);
}
