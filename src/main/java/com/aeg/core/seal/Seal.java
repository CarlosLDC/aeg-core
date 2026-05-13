package com.aeg.core.seal;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;

import java.time.OffsetDateTime;

@Entity
@Table(name = "precintos")
public class Seal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_impresora")
    private com.aeg.core.printer.Printer printer;

    @Column(name = "serial", nullable = false)
    private String serial;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "fecha_instalacion")
    private OffsetDateTime installationDate;

    @Column(name = "fecha_retiro")
    private OffsetDateTime removalDate;

    @Convert(converter = SealColorConverter.class)
    @Column(name = "color", nullable = false)
    private SealColor color;

    @Convert(converter = SealStatusConverter.class)
    @Column(name = "estatus", nullable = false)
    private SealStatus status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public com.aeg.core.printer.Printer getPrinter() { return printer; }
    public void setPrinter(com.aeg.core.printer.Printer printer) { this.printer = printer; }
    public Long getPrinterId() { return printer == null ? null : printer.getId(); }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getInstallationDate() { return installationDate; }
    public void setInstallationDate(OffsetDateTime installationDate) { this.installationDate = installationDate; }
    public OffsetDateTime getRemovalDate() { return removalDate; }
    public void setRemovalDate(OffsetDateTime removalDate) { this.removalDate = removalDate; }
    public SealColor getColor() { return color; }
    public void setColor(SealColor color) { this.color = color; }
    public SealStatus getStatus() { return status; }
    public void setStatus(SealStatus status) { this.status = status; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
