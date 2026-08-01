package com.aeg.core.enajenacion.mqtt.activity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

/**
 * In-memory ring buffer for unit tests (not a Spring bean; production uses JPA).
 */
public class InMemoryEnajenacionActivityPersistence implements EnajenacionActivityPersistence {

	static final int MAX_ENTRIES = 2_000;

	private final Deque<EnajenacionActivityEntry> entries = new ArrayDeque<>();

	@Override
	public synchronized void save(EnajenacionActivityEntry entry) {
		entries.addFirst(entry);
		while (entries.size() > MAX_ENTRIES) {
			entries.removeLast();
		}
	}

	@Override
	public synchronized List<EnajenacionActivityEntry> find(
			EnajenacionActivityQuery query, int limit, int page) {
		EnajenacionActivityQuery normalized = EnajenacionActivityQuery.normalize(query);
		int effectiveLimit = Math.max(1, limit);
		int effectivePage = Math.max(0, page);
		int skip = effectivePage * effectiveLimit;
		return filteredStream(normalized)
				.skip(skip)
				.limit(effectiveLimit)
				.toList();
	}

	@Override
	public synchronized long count(EnajenacionActivityQuery query) {
		EnajenacionActivityQuery normalized = EnajenacionActivityQuery.normalize(query);
		return filteredStream(normalized).count();
	}

	@Override
	public synchronized void clear() {
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
}
