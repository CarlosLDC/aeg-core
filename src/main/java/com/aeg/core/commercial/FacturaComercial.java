package com.aeg.core.commercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "facturas_comerciales")
public class FacturaComercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", length = 30)
    private String numeroFactura;

    @Column(name = "sufijo_control", length = 10)
    private String sufijoControl;

    @Column(name = "factura_cliente", length = 50)
    private String facturaCliente;

    @Column(name = "modalidad", nullable = false, length = 30)
    private String modalidad = "VENTA";

    @Column(name = "estado_cobro", nullable = false, length = 30)
    private String estadoCobro = "PAGADO";

    @Column(name = "promotor_vendedor", length = 100)
    private String promotorVendedor;

    @Column(name = "notas_comerciales", columnDefinition = "TEXT")
    private String notasComerciales;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (modalidad == null || modalidad.isBlank()) {
            modalidad = "VENTA";
        }
        if (estadoCobro == null || estadoCobro.isBlank()) {
            estadoCobro = "PAGADO";
        }
    }

    public Long getId() {
        return id;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getSufijoControl() {
        return sufijoControl;
    }

    public void setSufijoControl(String sufijoControl) {
        this.sufijoControl = sufijoControl;
    }

    public String getFacturaCliente() {
        return facturaCliente;
    }

    public void setFacturaCliente(String facturaCliente) {
        this.facturaCliente = facturaCliente;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public String getEstadoCobro() {
        return estadoCobro;
    }

    public void setEstadoCobro(String estadoCobro) {
        this.estadoCobro = estadoCobro;
    }

    public String getPromotorVendedor() {
        return promotorVendedor;
    }

    public void setPromotorVendedor(String promotorVendedor) {
        this.promotorVendedor = promotorVendedor;
    }

    public String getNotasComerciales() {
        return notasComerciales;
    }

    public void setNotasComerciales(String notasComerciales) {
        this.notasComerciales = notasComerciales;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
