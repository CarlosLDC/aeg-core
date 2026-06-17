package com.aeg.core.enajenacion.mqtt.activity;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.EnajenacionSession;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

@Component
public class EnajenacionActivityRecorder {

    private final EnajenacionActivityStore store;

    public EnajenacionActivityRecorder(EnajenacionActivityStore store) {
        this.store = store;
    }

    public void recordInbound(
            String topic,
            String payload,
            String mac,
            Long printerId,
            String ptrReg,
            EnajenacionActivityResult result,
            String detail,
            EnajenacionSessionState sessionState) {
        store.record(EnajenacionActivityEntry.create(
                mac,
                printerId,
                ptrReg,
                EnajenacionActivityDirection.INBOUND,
                topic,
                payload,
                result,
                detail,
                sessionState));
    }

    public void recordOutbound(
            String topic,
            String payload,
            EnajenacionSession session,
            EnajenacionActivityResult result,
            String detail) {
        store.record(EnajenacionActivityEntry.create(
                session.compactMac(),
                session.printerId(),
                session.context().fiscalSerial(),
                EnajenacionActivityDirection.OUTBOUND,
                topic,
                payload,
                result,
                detail,
                session.state()));
    }

    public void recordSessionEvent(
            EnajenacionSession session,
            EnajenacionActivityResult result,
            String detail,
            EnajenacionSessionState sessionState) {
        store.record(EnajenacionActivityEntry.create(
                session.compactMac(),
                session.printerId(),
                session.context().fiscalSerial(),
                null,
                null,
                null,
                result,
                detail,
                sessionState));
    }

    public void recordSessionEvent(
            String mac,
            Long printerId,
            String ptrReg,
            EnajenacionActivityResult result,
            String detail,
            EnajenacionSessionState sessionState) {
        store.record(EnajenacionActivityEntry.create(
                mac,
                printerId,
                ptrReg,
                null,
                null,
                null,
                result,
                detail,
                sessionState));
    }
}
