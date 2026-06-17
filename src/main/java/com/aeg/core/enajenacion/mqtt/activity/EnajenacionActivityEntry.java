package com.aeg.core.enajenacion.mqtt.activity;

import java.time.Instant;
import java.util.UUID;

import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

public record EnajenacionActivityEntry(
        String id,
        Instant at,
        String mac,
        Long printerId,
        String ptrReg,
        EnajenacionActivityDirection direction,
        String topic,
        String payload,
        EnajenacionActivityResult result,
        String detail,
        EnajenacionSessionState sessionState) {

    public static EnajenacionActivityEntry create(
            String mac,
            Long printerId,
            String ptrReg,
            EnajenacionActivityDirection direction,
            String topic,
            String payload,
            EnajenacionActivityResult result,
            String detail,
            EnajenacionSessionState sessionState) {
        return new EnajenacionActivityEntry(
                UUID.randomUUID().toString(),
                Instant.now(),
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
