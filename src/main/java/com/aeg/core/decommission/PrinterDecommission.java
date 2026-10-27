package com.aeg.core.decommission;

import java.time.LocalDate;
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
@Table(name = "impresora_desincorporaciones", schema = "public")
public class PrinterDecommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_impresora", nullable = false)
    private Printer printer;

    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false)
    private DecommissionReason reason;

    @Column(name = "es_propia", nullable = false)
    private boolean ownPrinter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private User user;

    @Column(name = "observaciones")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Printer getPrinter() { return printer; }
    public void setPrinter(Printer printer) { this.printer = printer; }
    public Long getPrinterId() { return printer == null ? null : printer.getId(); }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public DecommissionReason getReason() { return reason; }
    public void setReason(DecommissionReason reason) { this.reason = reason; }
    public boolean isOwnPrinter() { return ownPrinter; }
    public void setOwnPrinter(boolean ownPrinter) { this.ownPrinter = ownPrinter; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getUserId() { return user == null ? null : user.getId(); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (date == null) date = LocalDate.now();
    }
}
