package com.aeg.core.modificationrequest.client.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ClientModificationUpdateRequest(
		@NotNull Long clientId,
		@NotNull @Valid ClientModificationProposedData proposedData) {
}
