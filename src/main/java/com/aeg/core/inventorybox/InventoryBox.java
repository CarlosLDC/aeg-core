package com.aeg.core.inventorybox;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "cajas_inventario")
public class InventoryBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_caja", nullable = false, unique = true)
    private Integer boxNumber;

    @Column(name = "observaciones", length = 255)
    private String observations;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getBoxNumber() { return boxNumber; }
    public void setBoxNumber(Integer boxNumber) { this.boxNumber = boxNumber; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
