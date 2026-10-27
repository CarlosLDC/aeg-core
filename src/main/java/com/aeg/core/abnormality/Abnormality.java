package com.aeg.core.abnormality;

import java.time.OffsetDateTime;

import com.aeg.core.printer.Printer;
import com.aeg.core.security.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "anormalidades", schema = "public")
public class Abnormality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_impresora", nullable = false)
    private Printer printer;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private AbnormalityType type;

    @Column(name = "fecha_deteccion", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "autorizado", nullable = false)
    private boolean authorized;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private User user;

    @Column(name = "descripcion")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false)
    private AbnormalityOrigin origin = AbnormalityOrigin.MANUAL;

    @Column(name = "id_origen")
    private Long originId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Printer getPrinter() { return printer; }
    public void setPrinter(Printer printer) { this.printer = printer; }
    public Long getPrinterId() { return printer == null ? null : printer.getId(); }
    public AbnormalityType getType() { return type; }
    public void setType(AbnormalityType type) { this.type = type; }
    public OffsetDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(OffsetDateTime detectedAt) { this.detectedAt = detectedAt; }
    public boolean isAuthorized() { return authorized; }
    public void setAuthorized(boolean authorized) { this.authorized = authorized; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getUserId() { return user == null ? null : user.getId(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AbnormalityOrigin getOrigin() { return origin; }
    public void setOrigin(AbnormalityOrigin origin) { this.origin = origin; }
    public Long getOriginId() { return originId; }
    public void setOriginId(Long originId) { this.originId = originId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (detectedAt == null) detectedAt = now;
    }
}
