package com.aeg.core.tecnico.dto;

import jakarta.validation.constraints.NotNull;

public record TecnicoRequest(@NotNull Long employeeId) {
}
