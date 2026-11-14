package com.aeg.core.commercial.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommercialPaymentRequest {

    @NotNull
    private Long facturaComercialId;

    @NotBlank
    @Size(max = 30)
    private String metodoBanco;

    @Size(max = 100)
    private String referencia;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal monto;

    @Size(max = 100)
    private String cuentaDestino;
}
