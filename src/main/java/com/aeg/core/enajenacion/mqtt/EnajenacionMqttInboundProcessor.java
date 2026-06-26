package com.aeg.core.enajenacion.mqtt;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityRecorder;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Procesa mensajes fiscales entrantes con deduplicación breve para evitar doble
 * manejo cuando el broker reenvía al mismo servidor lo publicado por {@code /api/mqtt/publish}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnajenacionMqttInboundProcessor {

    private static final long DEDUPE_WINDOW_MS = 5_000L;

    private final EnajenacionMqttOrchestrator orchestrator;
    private final EnajenacionMqttSettings settings;
    private final EnajenacionSessionRegistry sessionRegistry;
    private final EnajenacionActivityRecorder activityRecorder;
    private final ConcurrentHashMap<String, Long> recentInbound = new ConcurrentHashMap<>();

    public Optional<EnajenacionStartOutcome> process(String topic, String payload) {
        if (!settings.enabled() || !FiscalMqttTopics.isFiscalInboundTopic(topic)) {
            return Optional.empty();
        }
        String compactMac = FiscalMqttTopics.extractCompactMac(topic).orElse(null);
        boolean awaitingDeviceResponse = compactMac != null
                && sessionRegistry.find(compactMac)
                        .filter(EnajenacionSession::isAwaitingResponse)
                        .isPresent();
        if (!awaitingDeviceResponse && isDuplicate(topic, payload)) {
            log.debug("Skipping duplicate enajenacion inbound topic={}", topic);
            String mac = compactMac != null ? compactMac : "";
            activityRecorder.recordInbound(
                    topic,
                    payload,
                    mac,
                    null,
                    null,
                    EnajenacionActivityResult.IGNORED,
                    "Duplicate inbound within dedupe window",
                    null);
            return Optional.empty();
        }
        return orchestrator.handleInboundWithOutcome(topic, payload);
    }

    private boolean isDuplicate(String topic, String payload) {
        long now = System.currentTimeMillis();
        String key = topic + "\u0000" + payload;
        Long previous = recentInbound.put(key, now);
        recentInbound.entrySet().removeIf(entry -> now - entry.getValue() > DEDUPE_WINDOW_MS);
        return previous != null && now - previous < DEDUPE_WINDOW_MS;
    }
}
