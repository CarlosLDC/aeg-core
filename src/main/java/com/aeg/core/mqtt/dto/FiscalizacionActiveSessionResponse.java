package com.aeg.core.mqtt.dto;

import java.time.Instant;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSession;
import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;

public record FiscalizacionActiveSessionResponse(
        String mac,
        Long printerId,
        String ptrReg,
        FiscalizacionSessionState state,
        Instant startedAt,
        String lastError,
        boolean awaitingResponse,
        Instant awaitingSince,
        Integer timeoutSeconds) {

    public static FiscalizacionActiveSessionResponse from(FiscalizacionSession session) {
        return new FiscalizacionActiveSessionResponse(
                session.compactMac(),
                session.printerId(),
                session.context().ptrReg(),
                session.state(),
                session.startedAt(),
                session.lastError(),
                session.isAwaitingResponse(),
                session.awaitingSince(),
                session.awaitingTimeoutSeconds());
    }
}
