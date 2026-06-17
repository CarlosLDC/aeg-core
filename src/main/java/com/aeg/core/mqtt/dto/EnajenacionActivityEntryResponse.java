package com.aeg.core.mqtt.dto;

import java.time.Instant;

import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityDirection;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityEntry;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;

public record EnajenacionActivityEntryResponse(
        String id,
        Instant at,
        String mac,
        Long printerId,
        String ptrReg,
        EnajenacionActivityDirection direction,
        String topic,
        String payload,
        EnajenacionActivityResult result,
        String detail,
        EnajenacionSessionState sessionState) {

    public static EnajenacionActivityEntryResponse from(EnajenacionActivityEntry entry) {
        return new EnajenacionActivityEntryResponse(
                entry.id(),
                entry.at(),
                entry.mac(),
                entry.printerId(),
                entry.ptrReg(),
                entry.direction(),
                entry.topic(),
                entry.payload(),
                entry.result(),
                entry.detail(),
                entry.sessionState());
    }
}
