package com.aeg.core.modificationrequest.software;

import java.util.List;
import java.util.Optional;

import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationCreateRequest;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestListItemResponse;

public interface SoftwareModificationRequestService {

    SoftwareModificationRequestDetailResponse createRequest(SoftwareModificationCreateRequest request);

    List<SoftwareModificationRequestListItemResponse> findByStatus(ModificationRequestStatus status);

    SoftwareModificationRequestDetailResponse findById(Long id);

    Optional<SoftwareModificationRequestDetailResponse> findPendingByPrinterId(Long printerId);

    SoftwareModificationRequestDetailResponse approve(Long id);

    SoftwareModificationRequestDetailResponse reject(Long id);

    SoftwareModificationRequestDetailResponse cancel(Long id);
}
