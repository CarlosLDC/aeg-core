package com.aeg.core.fiscalizacion.mqtt.activity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSession;
import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;

@Component
public class FiscalizacionActivityStore {

    private static final int MAX_ENTRIES = 2_000;

    private final Deque<FiscalizacionActivityEntry> entries = new ArrayDeque<>();

    public synchronized void record(FiscalizacionActivityEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    public void recordInbound(
            String topic,
            String payload,
            String mac,
            Long printerId,
            String ptrReg,
            FiscalizacionActivityResult result,
            String detail,
            FiscalizacionSessionState state) {
        record(FiscalizacionActivityEntry.create(
                mac, printerId, ptrReg, FiscalizacionActivityDirection.INBOUND,
                topic, payload, result, detail, state));
    }

    public void recordOutbound(
            String topic,
            String payload,
            FiscalizacionSession session,
            FiscalizacionActivityResult result,
            String detail) {
        record(FiscalizacionActivityEntry.create(
                session.compactMac(),
                session.printerId(),
                session.context().ptrReg(),
                FiscalizacionActivityDirection.OUTBOUND,
                topic,
                payload,
                result,
                detail,
                session.state()));
    }

    public void recordOutboundNoSession(
            String topic,
            String payload,
            String mac,
            String ptrReg,
            FiscalizacionActivityResult result,
            String detail) {
        record(FiscalizacionActivityEntry.create(
                mac, null, ptrReg, FiscalizacionActivityDirection.OUTBOUND,
                topic, payload, result, detail, null));
    }

    public void recordSessionEvent(
            FiscalizacionSession session,
            FiscalizacionActivityResult result,
            String detail,
            FiscalizacionSessionState state) {
        record(FiscalizacionActivityEntry.create(
                session.compactMac(),
                session.printerId(),
                session.context().ptrReg(),
                FiscalizacionActivityDirection.OUTBOUND,
                null,
                null,
                result,
                detail,
                state));
    }

    public synchronized List<FiscalizacionActivityEntry> find(String macFilter, int limit) {
        int clamped = Math.min(Math.max(1, limit), 500);
        String compact = macFilter == null || macFilter.isBlank()
                ? null
                : MacAddressNormalizer.toCompactForm(macFilter);
        List<FiscalizacionActivityEntry> out = new ArrayList<>();
        for (FiscalizacionActivityEntry entry : entries) {
            if (compact != null
                    && (entry.mac() == null
                            || !entry.mac().toUpperCase(Locale.ROOT).equals(compact))) {
                continue;
            }
            out.add(entry);
            if (out.size() >= clamped) {
                break;
            }
        }
        return out;
    }

    public synchronized long count(String macFilter) {
        return find(macFilter, MAX_ENTRIES).size();
    }

    public synchronized void clear() {
        entries.clear();
    }
}
