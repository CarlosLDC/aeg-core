package com.aeg.core.enajenacion.mqtt;

import java.time.Instant;

public final class VenezuelaNaiveUnixTimestamp {

    private static final long VENEZUELA_UTC_OFFSET_SECONDS = 4L * 3600L;

    private VenezuelaNaiveUnixTimestamp() {
    }

    public static long currentSeconds() {
        return Instant.now().getEpochSecond() - VENEZUELA_UTC_OFFSET_SECONDS;
    }

    public static long fromInstant(Instant instant) {
        return instant.getEpochSecond() - VENEZUELA_UTC_OFFSET_SECONDS;
    }
}
