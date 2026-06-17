package com.aeg.core.mqtt.dto;

import java.time.Instant;
import java.util.List;

public record EnajenacionActivityListResponse(
        List<EnajenacionActivityEntryResponse> entries,
        int total) {
}
