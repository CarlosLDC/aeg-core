package com.aeg.core.printer.logs;

import java.time.Instant;

/**
 * In-memory representation of an incoming printer log message awaiting batched persistence.
 */
public record PrinterLogEntry(
        String mac,
        String topic,
        String payload,
        Instant recordedAt
) {
}
