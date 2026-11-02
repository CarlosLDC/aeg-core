package com.aeg.core.tools.mqtt;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Actualización de firmware lanzada desde Tools cuyo resultado aún debe verificarse. */
@Entity
@Table(name = "firmware_update_attempts")
@Getter
@Setter
public class FirmwareUpdateAttempt {

    public enum Status {
        /** Esperando verificar la versión. */
        PENDING,
        /** La versión cambió y se guardó en la base de datos. */
        CONFIRMED,
        /** La impresora respondió, pero con la misma versión: no se aplicó. */
        FAILED,
        /** La impresora nunca respondió durante la ventana de verificación. */
        NO_RESPONSE,
        /** Reemplazada por una actualización más reciente. */
        SUPERSEDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_impresora", nullable = false)
    private Long printerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "previous_version")
    private String previousVersion;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "next_check_at", nullable = false)
    private OffsetDateTime nextCheckAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "got_reply", nullable = false)
    private boolean gotReply;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;
}
