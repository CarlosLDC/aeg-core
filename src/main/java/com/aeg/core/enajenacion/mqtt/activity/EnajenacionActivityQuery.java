package com.aeg.core.enajenacion.mqtt.activity;

public record EnajenacionActivityQuery(
        String mac,
        EnajenacionActivityResult result,
        String ptrRegContains,
        EnajenacionActivityDirection direction,
        boolean sessionEventsOnly) {

    public static EnajenacionActivityQuery unrestricted() {
        return new EnajenacionActivityQuery(null, null, null, null, false);
    }
}
