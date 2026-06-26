package com.aeg.core.enajenacion.mqtt.activity;

import java.time.Instant;

import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mqtt_enajenacion_activity")
public class MqttEnajenacionActivityLog {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 12)
    private String mac;

    @Column(name = "printer_id")
    private Long printerId;

    @Column(name = "ptr_reg", length = 32)
    private String ptrReg;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private EnajenacionActivityDirection direction;

    @Column(columnDefinition = "text")
    private String topic;

    @Column(columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnajenacionActivityResult result;

    @Column(columnDefinition = "text")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_state", length = 32)
    private EnajenacionSessionState sessionState;

    protected MqttEnajenacionActivityLog() {
    }

    public static MqttEnajenacionActivityLog from(EnajenacionActivityEntry entry) {
        MqttEnajenacionActivityLog entity = new MqttEnajenacionActivityLog();
        entity.id = entry.id();
        entity.recordedAt = entry.at();
        entity.mac = entry.mac();
        entity.printerId = entry.printerId();
        entity.ptrReg = entry.ptrReg();
        entity.direction = entry.direction();
        entity.topic = entry.topic();
        entity.payload = entry.payload();
        entity.result = entry.result();
        entity.detail = entry.detail();
        entity.sessionState = entry.sessionState();
        return entity;
    }

    public EnajenacionActivityEntry toEntry() {
        return new EnajenacionActivityEntry(
                id,
                recordedAt,
                mac,
                printerId,
                ptrReg,
                direction,
                topic,
                payload,
                result,
                detail,
                sessionState);
    }
}
