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

import java.time.OffsetDateTime;

@Entity
@Table(name = "facturas_comerciales")
public class FacturaComercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, length = 30)
    private String numeroFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_distribuidora")
    private com.aeg.core.distributor.Distributor distributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private com.aeg.core.client.Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_comercial")
    private ClienteComercial clienteComercial;

    @Column(name = "modalidad", nullable = false, length = 30)
    private String modalidad = "VENTA";

    @Column(name = "estado_cobro", nullable = false, length = 30)
    private String estadoCobro = "PAGADO";

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

    public com.aeg.core.distributor.Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(com.aeg.core.distributor.Distributor distributor) {
        this.distributor = distributor;
    }

    public com.aeg.core.client.Client getClient() {
        return client;
    }

    public void setClient(com.aeg.core.client.Client client) {
        this.client = client;
    }

    public ClienteComercial getClienteComercial() {
        return clienteComercial;
    }

    public void setClienteComercial(ClienteComercial clienteComercial) {
        this.clienteComercial = clienteComercial;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
