package com.aeg.core.distributorperson.dto;

import java.time.OffsetDateTime;

public record DistributorPersonResponse(Long id, Long employeeId, OffsetDateTime createdAt) {
}
