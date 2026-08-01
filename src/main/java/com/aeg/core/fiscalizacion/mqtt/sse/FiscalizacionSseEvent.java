package com.aeg.core.fiscalizacion.mqtt.sse;

import java.time.Instant;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;

public record FiscalizacionSseEvent(
        FiscalizacionSseEventType type,
        String mac,
        Instant at,
        Long printerId,
        String ptrReg,
        String acceptedStepId,
        String publishedStepId,
        String acceptedRespuestaTopic,
        String acceptedRespuestaPayload,
        String comandoTopic,
        String comandoPayload,
        FiscalizacionSessionState sessionState,
        String reason,
        FiscalizacionSessionState failedAtState) {

    public static FiscalizacionSseEvent connected(String mac) {
        return new FiscalizacionSseEvent(
                FiscalizacionSseEventType.CONNECTED, mac, Instant.now(),
                null, null, null, null, null, null, null, null, null, null, null);
    }

    public static FiscalizacionSseEvent sessionStarted(
            String mac, String ptrReg, String publishedStepId,
            String comandoTopic, String comandoPayload, FiscalizacionSessionState state) {
        return new FiscalizacionSseEvent(
                FiscalizacionSseEventType.SESSION_STARTED, mac, Instant.now(),
                null, ptrReg, null, publishedStepId, null, null,
                comandoTopic, comandoPayload, state, null, null);
    }

    public static FiscalizacionSseEvent stepTransition(
            String mac, Long printerId, String ptrReg,
            String acceptedStepId, String publishedStepId,
            String acceptedRespuestaTopic, String acceptedRespuestaPayload,
            String comandoTopic, String comandoPayload, FiscalizacionSessionState state) {
        return new FiscalizacionSseEvent(
                FiscalizacionSseEventType.STEP_TRANSITION, mac, Instant.now(),
                printerId, ptrReg, acceptedStepId, publishedStepId,
                acceptedRespuestaTopic, acceptedRespuestaPayload,
                comandoTopic, comandoPayload, state, null, null);
    }

    public static FiscalizacionSseEvent sessionCompleted(String mac, Long printerId, String ptrReg) {
        return new FiscalizacionSseEvent(
                FiscalizacionSseEventType.SESSION_COMPLETED, mac, Instant.now(),
                printerId, ptrReg, null, null, null, null, null, null,
                FiscalizacionSessionState.COMPLETED, null, null);
    }

    public static FiscalizacionSseEvent sessionFailed(
            String mac, Long printerId, String ptrReg, String reason,
            FiscalizacionSessionState failedAtState) {
        return new FiscalizacionSseEvent(
                FiscalizacionSseEventType.SESSION_FAILED, mac, Instant.now(),
                printerId, ptrReg, null, null, null, null, null, null,
                FiscalizacionSessionState.FAILED, reason, failedAtState);
    }
}
