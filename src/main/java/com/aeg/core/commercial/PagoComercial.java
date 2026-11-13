package com.aeg.core.commercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pagos_comerciales")
public class PagoComercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_factura_comercial", nullable = false)
    private FacturaComercial facturaComercial;

    @Column(name = "metodo_banco", nullable = false, length = 30)
    private String metodoBanco;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "monto", precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "cuenta_destino", length = 100)
    private String cuentaDestino;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public FacturaComercial getFacturaComercial() {
        return facturaComercial;
    }

    public void setFacturaComercial(FacturaComercial facturaComercial) {
        this.facturaComercial = facturaComercial;
    }

    public Long getFacturaComercialId() {
        return facturaComercial != null ? facturaComercial.getId() : null;
    }

    public String getMetodoBanco() {
        return metodoBanco;
    }

    public void setMetodoBanco(String metodoBanco) {
        this.metodoBanco = metodoBanco;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
