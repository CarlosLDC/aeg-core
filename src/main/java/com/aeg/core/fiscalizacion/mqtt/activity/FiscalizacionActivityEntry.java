package com.aeg.core.fiscalizacion.mqtt.activity;

import java.time.Instant;
import java.util.UUID;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;

public record FiscalizacionActivityEntry(
        String id,
        Instant at,
        String mac,
        Long printerId,
        String ptrReg,
        FiscalizacionActivityDirection direction,
        String topic,
        String payload,
        FiscalizacionActivityResult result,
        String detail,
        FiscalizacionSessionState sessionState) {

    public static FiscalizacionActivityEntry create(
            String mac,
            Long printerId,
            String ptrReg,
            FiscalizacionActivityDirection direction,
            String topic,
            String payload,
            FiscalizacionActivityResult result,
            String detail,
            FiscalizacionSessionState sessionState) {
        return new FiscalizacionActivityEntry(
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
