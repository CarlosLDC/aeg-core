package com.aeg.core.mqtt.dto;

import java.time.Instant;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;
import com.aeg.core.fiscalizacion.mqtt.activity.FiscalizacionActivityDirection;
import com.aeg.core.fiscalizacion.mqtt.activity.FiscalizacionActivityEntry;
import com.aeg.core.fiscalizacion.mqtt.activity.FiscalizacionActivityResult;

public record FiscalizacionActivityEntryResponse(
        String id,
        Instant at,
        String mac,
        Long printerId,
        String ptrReg,
        FiscalizacionActivityDirection direction,
        String topic,
        String payload,
        FiscalizacionActivityResult result,
        String detail,
        FiscalizacionSessionState sessionState) {

    public static FiscalizacionActivityEntryResponse from(FiscalizacionActivityEntry entry) {
        return new FiscalizacionActivityEntryResponse(
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
