package com.aeg.core.mqtt.dto;

import java.time.Instant;

import com.aeg.core.enajenacion.mqtt.EnajenacionSession;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

public record EnajenacionActiveSessionResponse(
        String mac,
        Long printerId,
        String ptrReg,
        EnajenacionSessionState state,
        Instant startedAt,
        String lastError,
        boolean awaitingResponse,
        Instant awaitingSince,
        Integer timeoutSeconds) {

    public static EnajenacionActiveSessionResponse from(EnajenacionSession session) {
        return new EnajenacionActiveSessionResponse(
                session.compactMac(),
                session.printerId(),
                session.context().fiscalSerial(),
                session.state(),
                session.startedAt(),
                session.lastError(),
                session.isAwaitingResponse(),
                session.awaitingSince(),
                session.awaitingTimeoutSeconds());
    }
}
