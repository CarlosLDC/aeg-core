package com.aeg.core.enajenacion.mqtt.sse;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnajenacionSseHeartbeatScheduler {

    private final EnajenacionSseBroadcaster broadcaster;

    @Scheduled(fixedDelayString = "${app.mqtt.enajenacion.sse-heartbeat-ms:20000}")
    void heartbeat() {
        broadcaster.sendHeartbeat();
    }
}
