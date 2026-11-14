package com.aeg.core.commercial.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialInvoiceResponse {

    private Long id;
    private String numeroFactura;
    private Long distribuidorId;
    private String distribuidorNombre;
    private Long clienteId;
    private String clienteNombre;
    private Long clienteComercialId;
    private String clienteComercialNombre;
    private String modalidad;
    private String estadoCobro;
    private OffsetDateTime createdAt;
    private long paymentCount;
    private BigDecimal paidAmountTotal;
    private long printerCount;
}
