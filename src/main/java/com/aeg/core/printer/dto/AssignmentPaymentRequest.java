package com.aeg.core.printer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentPaymentRequest(
        @NotBlank @Size(max = 30) String metodoBanco,
        @Size(max = 100) String referencia,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal monto,
        @Size(max = 100) String cuentaDestino,
        LocalDate fechaPago,
        @Size(max = 500) String notas
) {
}
