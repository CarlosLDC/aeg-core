package com.aeg.core.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialInvoiceRequest {

    @NotBlank(message = "El número de factura es obligatorio.")
    @Size(max = 30, message = "El número de factura no puede superar los 30 caracteres.")
    private String numeroFactura;

    private Long distribuidorId;

    private Long clienteId;

    @NotBlank(message = "La modalidad es obligatoria.")
    @Size(max = 30, message = "La modalidad no puede superar los 30 caracteres.")
    private String modalidad;

    @NotBlank(message = "El estado de cobro es obligatorio.")
    @Size(max = 30, message = "El estado de cobro no puede superar los 30 caracteres.")
    private String estadoCobro;
}
