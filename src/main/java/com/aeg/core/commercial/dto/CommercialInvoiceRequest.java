package com.aeg.core.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommercialInvoiceRequest {

    @Size(max = 30)
    private String numeroFactura;

    @Size(max = 10)
    private String sufijoControl;

    private Long clienteComercialId;

    @NotBlank
    @Size(max = 30)
    private String modalidad;

    @NotBlank
    @Size(max = 30)
    private String estadoCobro;

    @Size(max = 5000)
    private String notasComerciales;
}
