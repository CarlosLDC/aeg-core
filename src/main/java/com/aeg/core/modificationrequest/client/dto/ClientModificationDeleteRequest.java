package com.aeg.core.modificationrequest.client.dto;

import jakarta.validation.constraints.NotNull;

public record ClientModificationDeleteRequest(@NotNull Long clientId) {
}
