package com.aeg.core.distribuidor.dto;

import jakarta.validation.constraints.NotNull;

public record DistribuidorRequest(@NotNull Long employeeId) {
}
