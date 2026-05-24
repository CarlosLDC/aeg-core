package com.aeg.core.modificationrequest.dto;

import jakarta.validation.constraints.NotNull;

public record EmployeeModificationDeleteRequest(@NotNull Long employeeId) {
}
