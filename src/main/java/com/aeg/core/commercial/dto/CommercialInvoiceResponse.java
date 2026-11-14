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
    private String sufijoControl;
    private String facturaCliente;
    private String modalidad;
    private String estadoCobro;
    private String promotorVendedor;
    private String notasComerciales;
    private OffsetDateTime createdAt;
    private long paymentCount;
    private BigDecimal paidAmountTotal;
    private long printerCount;
}
