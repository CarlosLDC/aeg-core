package com.aeg.core.fiscalizacion.mqtt.sse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FiscalizacionSseBroadcaster {

    static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<SseEmitter>> emittersByMac = new ConcurrentHashMap<>();

    public FiscalizacionSseBroadcaster(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(String compactMac) {
        String mac = normalizeMac(compactMac);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByMac.computeIfAbsent(mac, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        Runnable cleanup = () -> unsubscribe(mac, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());
        return emitter;
    }

    public void broadcast(String compactMac, FiscalizacionSseEvent event) {
        String mac = normalizeMac(compactMac);
        Set<SseEmitter> emitters = emittersByMac.get(mac);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        SseEmitter.SseEventBuilder builder;
        try {
            builder = SseEmitter.event()
                    .name(wireEventName(event.type()))
                    .data(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize fiscalizacion SSE event for mac={}: {}", mac, ex.getMessage());
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(builder);
            } catch (IOException | IllegalStateException ex) {
                unsubscribe(mac, emitter);
            }
        }
    }

    private void unsubscribe(String mac, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByMac.get(mac);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByMac.remove(mac, emitters);
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // already closed
        }
    }

    private static String normalizeMac(String compactMac) {
        return compactMac == null ? "" : compactMac.trim().toUpperCase();
    }

    private static String wireEventName(FiscalizacionSseEventType type) {
        return switch (type) {
            case CONNECTED -> "connected";
            case SESSION_STARTED -> "session_started";
            case STEP_TRANSITION -> "step_transition";
            case SESSION_COMPLETED -> "session_completed";
            case SESSION_FAILED -> "session_failed";
        };
    }
}
