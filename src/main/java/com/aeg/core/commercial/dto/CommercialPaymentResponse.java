package com.aeg.core.commercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialPaymentResponse {

    private Long id;
    private Long facturaComercialId;
    private String numeroFactura;
    private Long empresaId;
    private Long clienteComercialId;
    private String clienteComercialNombre;
    private String metodoBanco;
    private String referencia;
    private BigDecimal monto;
    private String cuentaDestino;
    private LocalDate fechaPago;
    private String notas;
    private Long registeredById;
    private String registeredByName;
    private OffsetDateTime createdAt;
}
