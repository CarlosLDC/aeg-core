package com.aeg.core.client.dto;

import java.time.LocalDateTime;

public record ClientResponse(

	Long id,
	Long branchId,
	LocalDateTime createdAt
) {
}