package com.aeg.core.enajenacion.mqtt.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

@Component
public class EnajenacionActivityStore {

    private final int capacity;
    private final Deque<EnajenacionActivityEntry> entries = new ConcurrentLinkedDeque<>();

    public EnajenacionActivityStore(
            @Value("${app.mqtt.enajenacion.activity.buffer-size:300}") int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public void record(EnajenacionActivityEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > capacity) {
            entries.pollLast();
        }
    }

    public List<EnajenacionActivityEntry> recent(int limit, String macFilter) {
        int effectiveLimit = Math.min(Math.max(1, limit), capacity);
        String normalizedMac = normalizeMacFilter(macFilter);
        List<EnajenacionActivityEntry> snapshot = new ArrayList<>(entries);
        if (normalizedMac != null) {
            snapshot = snapshot.stream()
                    .filter(entry -> normalizedMac.equals(entry.mac()))
                    .toList();
        }
        if (snapshot.size() <= effectiveLimit) {
            return List.copyOf(snapshot);
        }
        return Collections.unmodifiableList(snapshot.subList(0, effectiveLimit));
    }

    public void clear() {
        entries.clear();
    }

    private static String normalizeMacFilter(String macFilter) {
        if (macFilter == null || macFilter.isBlank()) {
            return null;
        }
        return MacAddressNormalizer.toCompactForm(macFilter);
    }
}
