package com.aeg.core.fiscalizacion.mqtt.sse;

import org.springframework.stereotype.Component;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionConstants;
import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSession;
import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FiscalizacionSseNotifier {

    private final FiscalizacionSseBroadcaster broadcaster;

    public void notifyConnected(String compactMac) {
        broadcaster.broadcast(compactMac, FiscalizacionSseEvent.connected(compactMac));
    }

    public void notifySessionStarted(FiscalizacionSession session, String comandoTopic, String comandoPayload) {
        broadcaster.broadcast(
                session.compactMac(),
                FiscalizacionSseEvent.sessionStarted(
                        session.compactMac(),
                        session.context().ptrReg(),
                        FiscalizacionConstants.STEP_ACK,
                        comandoTopic,
                        comandoPayload,
                        session.state()));
    }

    public void notifyResultAccepted(
            FiscalizacionSession session, String respuestaTopic, String respuestaPayload,
            String comandoTopic, String comandoPayload) {
        broadcaster.broadcast(
                session.compactMac(),
                FiscalizacionSseEvent.stepTransition(
                        session.compactMac(),
                        session.printerId(),
                        session.context().ptrReg(),
                        FiscalizacionConstants.STEP_RESULT,
                        FiscalizacionConstants.STEP_CONFIG_SPIFFS,
                        respuestaTopic,
                        respuestaPayload,
                        comandoTopic,
                        comandoPayload,
                        session.state()));
    }

    public void notifyConfigAccepted(
            FiscalizacionSession session, String respuestaTopic, String respuestaPayload) {
        broadcaster.broadcast(
                session.compactMac(),
                FiscalizacionSseEvent.stepTransition(
                        session.compactMac(),
                        session.printerId(),
                        session.context().ptrReg(),
                        FiscalizacionConstants.STEP_CONFIG_SPIFFS,
                        null,
                        respuestaTopic,
                        respuestaPayload,
                        null,
                        null,
                        session.state()));
    }

    public void notifySessionCompleted(FiscalizacionSession session) {
        broadcaster.broadcast(
                session.compactMac(),
                FiscalizacionSseEvent.sessionCompleted(
                        session.compactMac(),
                        session.printerId(),
                        session.context().ptrReg()));
    }

    /**
     * {@code ptrFiscalizar} rechazado en la validación previa (sin sesión): se notifica como
     * {@code session_failed} con {@code failedAtState} nulo para que el asistente USB vea el motivo.
     */
    public void notifyRejected(String compactMac, String ptrReg, String reason) {
        broadcaster.broadcast(
                compactMac,
                FiscalizacionSseEvent.sessionFailed(compactMac, null, ptrReg, reason, null));
    }

    public void notifySessionFailed(
            FiscalizacionSession session, String reason, FiscalizacionSessionState failedAtState) {
        broadcaster.broadcast(
                session.compactMac(),
                FiscalizacionSseEvent.sessionFailed(
                        session.compactMac(),
                        session.printerId(),
                        session.context().ptrReg(),
                        reason,
                        failedAtState));
    }
}
