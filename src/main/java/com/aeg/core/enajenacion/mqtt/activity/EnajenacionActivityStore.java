package com.aeg.core.enajenacion.mqtt.activity;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class EnajenacionActivityStore {

    private final EnajenacionActivityPersistence persistence;

    public EnajenacionActivityStore(EnajenacionActivityPersistence persistence) {
        this.persistence = persistence;
    }

    public void record(EnajenacionActivityEntry entry) {
        persistence.save(entry);
    }

    public List<EnajenacionActivityEntry> find(EnajenacionActivityQuery query, int limit, int page) {
        EnajenacionActivityQuery normalized = JpaEnajenacionActivityPersistence.normalizeQuery(query);
        return persistence.find(normalized, clampLimit(limit), Math.max(0, page));
    }

    public long count(EnajenacionActivityQuery query) {
        EnajenacionActivityQuery normalized = JpaEnajenacionActivityPersistence.normalizeQuery(query);
        return persistence.count(normalized);
    }

    /** @deprecated use {@link #find(EnajenacionActivityQuery, int, int)} */
    public List<EnajenacionActivityEntry> recent(int limit, String macFilter) {
        EnajenacionActivityQuery query = macFilter == null || macFilter.isBlank()
                ? EnajenacionActivityQuery.unrestricted()
                : new EnajenacionActivityQuery(macFilter, null, null, null, false);
        return find(query, limit, 0);
    }

    public void clear() {
        persistence.clear();
    }

    private static int clampLimit(int limit) {
        return Math.min(Math.max(1, limit), 500);
    }
}
