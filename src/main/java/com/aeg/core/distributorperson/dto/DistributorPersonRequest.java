package com.aeg.core.distributorperson.dto;

import jakarta.validation.constraints.NotNull;

public record DistributorPersonRequest(@NotNull Long employeeId) {
}
