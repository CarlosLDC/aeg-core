package com.aeg.core.tecnico.dto;

import java.time.OffsetDateTime;

public record TecnicoResponse(Long id, Long employeeId, OffsetDateTime createdAt) {
}
