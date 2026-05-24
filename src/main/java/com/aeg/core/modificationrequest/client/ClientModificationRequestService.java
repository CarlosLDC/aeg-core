package com.aeg.core.modificationrequest.client;

import java.util.List;

import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.client.dto.ClientModificationProposedData;
import com.aeg.core.modificationrequest.client.dto.ClientModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.client.dto.ClientModificationRequestListItemResponse;

public interface ClientModificationRequestService {

	ClientModificationRequestDetailResponse requestUpdate(Long clientId, ClientModificationProposedData proposedData);

	ClientModificationRequestDetailResponse requestDelete(Long clientId);

	List<ClientModificationRequestListItemResponse> findByStatus(ModificationRequestStatus status);

	ClientModificationRequestDetailResponse findById(Long id);

	ClientModificationRequestDetailResponse approve(Long id);

	ClientModificationRequestDetailResponse reject(Long id);
}
