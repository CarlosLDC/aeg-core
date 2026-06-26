package com.aeg.core.enajenacion.mqtt.activity;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

/**
 * In-memory persistence for unit tests (no capacity eviction).
 */
public final class InMemoryEnajenacionActivityPersistence implements EnajenacionActivityPersistence {

    private final Deque<EnajenacionActivityEntry> entries = new ConcurrentLinkedDeque<>();

    @Override
    public void save(EnajenacionActivityEntry entry) {
        entries.addFirst(entry);
    }

    @Override
    public List<EnajenacionActivityEntry> find(EnajenacionActivityQuery query, int limit, int page) {
        EnajenacionActivityQuery normalized = JpaEnajenacionActivityPersistence.normalizeQuery(query);
        int effectiveLimit = Math.max(1, limit);
        int effectivePage = Math.max(0, page);
        int skip = effectivePage * effectiveLimit;
        return filteredStream(normalized)
                .skip(skip)
                .limit(effectiveLimit)
                .toList();
    }

    @Override
    public long count(EnajenacionActivityQuery query) {
        EnajenacionActivityQuery normalized = JpaEnajenacionActivityPersistence.normalizeQuery(query);
        return filteredStream(normalized).count();
    }

    @Override
    public void clear() {
        entries.clear();
    }

    private Stream<EnajenacionActivityEntry> filteredStream(EnajenacionActivityQuery query) {
        List<EnajenacionActivityEntry> snapshot = new ArrayList<>(entries);
        return snapshot.stream().filter(entry -> matches(entry, query));
    }

    private static boolean matches(EnajenacionActivityEntry entry, EnajenacionActivityQuery query) {
        if (query.mac() != null && !query.mac().equals(entry.mac())) {
            return false;
        }
        if (query.result() != null && query.result() != entry.result()) {
            return false;
        }
        if (query.ptrRegContains() != null) {
            String ptrReg = entry.ptrReg();
            if (ptrReg == null) {
                return false;
            }
            if (!ptrReg.toUpperCase().contains(query.ptrRegContains().toUpperCase())) {
                return false;
            }
        }
        if (query.sessionEventsOnly()) {
            return entry.direction() == null;
        }
        if (query.direction() != null && query.direction() != entry.direction()) {
            return false;
        }
        return true;
    }

    static String normalizeMac(String macFilter) {
        if (macFilter == null || macFilter.isBlank()) {
            return null;
        }
        return MacAddressNormalizer.toCompactForm(macFilter);
    }
}
