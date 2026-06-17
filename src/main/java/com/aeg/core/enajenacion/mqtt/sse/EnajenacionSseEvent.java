package com.aeg.core.enajenacion.mqtt.sse;

import java.time.Instant;

import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

public record EnajenacionSseEvent(
        EnajenacionSseEventType type,
        String mac,
        Instant at,
        Long printerId,
        String ptrReg,
        String acceptedStepId,
        String publishedStepId,
        String comandoTopic,
        String comandoPayload,
        EnajenacionSessionState sessionState,
        String reason,
        EnajenacionSessionState failedAtState) {

    public static EnajenacionSseEvent connected(String mac) {
        return new EnajenacionSseEvent(
                EnajenacionSseEventType.CONNECTED,
                mac,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static EnajenacionSseEvent sessionStarted(
            String mac,
            Long printerId,
            String ptrReg,
            String publishedStepId,
            String comandoTopic,
            String comandoPayload,
            EnajenacionSessionState sessionState) {
        return new EnajenacionSseEvent(
                EnajenacionSseEventType.SESSION_STARTED,
                mac,
                Instant.now(),
                printerId,
                ptrReg,
                null,
                publishedStepId,
                comandoTopic,
                comandoPayload,
                sessionState,
                null,
                null);
    }

    public static EnajenacionSseEvent stepTransition(
            String mac,
            Long printerId,
            String ptrReg,
            String acceptedStepId,
            String publishedStepId,
            String comandoTopic,
            String comandoPayload,
            EnajenacionSessionState sessionState) {
        return new EnajenacionSseEvent(
                EnajenacionSseEventType.STEP_TRANSITION,
                mac,
                Instant.now(),
                printerId,
                ptrReg,
                acceptedStepId,
                publishedStepId,
                comandoTopic,
                comandoPayload,
                sessionState,
                null,
                null);
    }

    public static EnajenacionSseEvent sessionCompleted(String mac, Long printerId, String ptrReg) {
        return new EnajenacionSseEvent(
                EnajenacionSseEventType.SESSION_COMPLETED,
                mac,
                Instant.now(),
                printerId,
                ptrReg,
                null,
                null,
                null,
                null,
                EnajenacionSessionState.COMPLETED,
                null,
                null);
    }

    public static EnajenacionSseEvent sessionFailed(
            String mac,
            Long printerId,
            String ptrReg,
            String reason,
            EnajenacionSessionState failedAtState) {
        return new EnajenacionSseEvent(
                EnajenacionSseEventType.SESSION_FAILED,
                mac,
                Instant.now(),
                printerId,
                ptrReg,
                null,
                null,
                null,
                null,
                EnajenacionSessionState.FAILED,
                reason,
                failedAtState);
    }
}
