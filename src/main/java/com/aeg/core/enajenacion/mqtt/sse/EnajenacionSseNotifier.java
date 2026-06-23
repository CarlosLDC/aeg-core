package com.aeg.core.enajenacion.mqtt.sse;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.EnajenacionFlowStepIds;
import com.aeg.core.enajenacion.mqtt.EnajenacionSession;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnajenacionSseNotifier {

    private final EnajenacionSseBroadcaster broadcaster;

    public void notifyConnected(String compactMac) {
        broadcaster.broadcast(compactMac, EnajenacionSseEvent.connected(compactMac));
    }

    public void notifySessionStarted(EnajenacionSession session, String comandoTopic, String comandoPayload) {
        broadcaster.broadcast(
                session.compactMac(),
                EnajenacionSseEvent.sessionStarted(
                        session.compactMac(),
                        session.printerId(),
                        session.context().fiscalSerial(),
                        EnajenacionFlowStepIds.DNF,
                        comandoTopic,
                        comandoPayload,
                        session.state()));
    }

    public void notifyStepTransition(
            EnajenacionSession session,
            EnajenacionSessionState acceptedFromState,
            String acceptedRespuestaTopic,
            String acceptedRespuestaPayload,
            String comandoTopic,
            String comandoPayload) {
        String acceptedStepId = EnajenacionFlowStepIds.acceptedStepId(acceptedFromState).orElse(null);
        String publishedStepId = EnajenacionFlowStepIds.publishedStepId(session.state()).orElse(null);
        broadcaster.broadcast(
                session.compactMac(),
                EnajenacionSseEvent.stepTransition(
                        session.compactMac(),
                        session.printerId(),
                        session.context().fiscalSerial(),
                        acceptedStepId,
                        publishedStepId,
                        acceptedRespuestaTopic,
                        acceptedRespuestaPayload,
                        comandoTopic,
                        comandoPayload,
                        session.state()));
    }

    public void notifyReportZAccepted(
            EnajenacionSession session, String acceptedRespuestaTopic, String acceptedRespuestaPayload) {
        broadcaster.broadcast(
                session.compactMac(),
                EnajenacionSseEvent.stepTransition(
                        session.compactMac(),
                        session.printerId(),
                        session.context().fiscalSerial(),
                        EnajenacionFlowStepIds.REPORT_Z,
                        null,
                        acceptedRespuestaTopic,
                        acceptedRespuestaPayload,
                        null,
                        null,
                        EnajenacionSessionState.COMPLETED));
    }

    public void notifySessionCompleted(EnajenacionSession session) {
        broadcaster.broadcast(
                session.compactMac(),
                EnajenacionSseEvent.sessionCompleted(
                        session.compactMac(),
                        session.printerId(),
                        session.context().fiscalSerial()));
    }

    public void notifySessionFailed(
            EnajenacionSession session,
            String reason,
            EnajenacionSessionState failedAtState) {
        broadcaster.broadcast(
                session.compactMac(),
                EnajenacionSseEvent.sessionFailed(
                        session.compactMac(),
                        session.printerId(),
                        session.context().fiscalSerial(),
                        reason,
                        failedAtState));
    }
}
