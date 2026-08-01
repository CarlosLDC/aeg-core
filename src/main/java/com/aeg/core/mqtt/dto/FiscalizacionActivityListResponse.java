package com.aeg.core.mqtt.dto;

import java.util.List;

public record FiscalizacionActivityListResponse(
        List<FiscalizacionActivityEntryResponse> entries,
        int total) {
}
