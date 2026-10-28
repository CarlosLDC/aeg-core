package com.aeg.core.printer.logs;

import java.util.List;

public record PrinterLogsPageResponse(
        Long printerId,
        String mac,
        long total,
        int page,
        int limit,
        List<PrinterMqttLogItem> logs
) {
}
