package com.aeg.core.printer.logs;

import java.time.Instant;

public record PrinterMqttLogItem(
        Long id,
        Instant recordedAt,
        Long printerId,
        String mac,
        String topic,
        String payload
) {
}
