package com.aeg.core.technician.dto;

import java.time.OffsetDateTime;

public record TechnicianResponse(Long id, Long employeeId, OffsetDateTime createdAt) {
}
