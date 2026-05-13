package com.aeg.core.distribuidor.dto;

import java.time.OffsetDateTime;

public record DistribuidorResponse(Long id, Long employeeId, OffsetDateTime createdAt) {
}
