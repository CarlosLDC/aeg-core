package com.aeg.core.technician.dto;

import jakarta.validation.constraints.NotNull;

public record TechnicianRequest(@NotNull Long employeeId) {
}
