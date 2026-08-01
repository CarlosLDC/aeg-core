package com.aeg.core.enajenacion.mqtt.activity;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

public record EnajenacionActivityQuery(
		String mac,
		EnajenacionActivityResult result,
		String ptrRegContains,
		EnajenacionActivityDirection direction,
		boolean sessionEventsOnly) {

	public static EnajenacionActivityQuery unrestricted() {
		return new EnajenacionActivityQuery(null, null, null, null, false);
	}

	public static EnajenacionActivityQuery normalize(EnajenacionActivityQuery query) {
		String mac = normalizeMacFilter(query.mac());
		String ptrReg = normalizePtrRegFilter(query.ptrRegContains());
		return new EnajenacionActivityQuery(
				mac,
				query.result(),
				ptrReg,
				query.direction(),
				query.sessionEventsOnly());
	}

	private static String normalizeMacFilter(String macFilter) {
		if (macFilter == null || macFilter.isBlank()) {
			return null;
		}
		return MacAddressNormalizer.toCompactForm(macFilter);
	}

	private static String normalizePtrRegFilter(String ptrRegFilter) {
		if (ptrRegFilter == null || ptrRegFilter.isBlank()) {
			return null;
		}
		return ptrRegFilter.trim();
	}
}
