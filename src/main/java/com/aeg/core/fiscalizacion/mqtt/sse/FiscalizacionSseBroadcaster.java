package com.aeg.core.fiscalizacion.mqtt.sse;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Difunde eventos SSE de fiscalización. Los clientes pueden suscribirse por MAC (panel Remoto) o por
 * registro fiscal {@code ptrReg} (asistente USB, que puede no conocer la MAC de antemano).
 */
@Component
@Slf4j
public class FiscalizacionSseBroadcaster {

    static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private static final String MAC_PREFIX = "mac:";
    private static final String REG_PREFIX = "reg:";

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<SseEmitter>> emittersByKey = new ConcurrentHashMap<>();

    public FiscalizacionSseBroadcaster(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(String compactMac) {
        return subscribeKey(macKey(compactMac));
    }

    public SseEmitter subscribeByPtrReg(String ptrReg) {
        return subscribeKey(regKey(ptrReg));
    }

    /**
     * Envía el evento a los suscriptores de la MAC y, si el evento trae {@code ptrReg}, también a los
     * suscriptores de ese registro.
     */
    public void broadcast(String compactMac, FiscalizacionSseEvent event) {
        SseEmitter.SseEventBuilder builder = toBuilder(event);
        if (builder == null) {
            return;
        }
        sendToKey(macKey(compactMac), builder);
        if (event.ptrReg() != null && !event.ptrReg().isBlank()) {
            sendToKey(regKey(event.ptrReg()), builder);
        }
    }

    /** Envía un evento solo a un emisor concreto (p. ej. {@code connected} al suscribirse por ptrReg). */
    public void sendTo(SseEmitter emitter, FiscalizacionSseEvent event) throws IOException {
        SseEmitter.SseEventBuilder builder = toBuilder(event);
        if (builder != null) {
            emitter.send(builder);
        }
    }

    private SseEmitter subscribeKey(String key) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByKey.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        Runnable cleanup = () -> unsubscribe(key, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());
        return emitter;
    }

    private SseEmitter.SseEventBuilder toBuilder(FiscalizacionSseEvent event) {
        try {
            return SseEmitter.event()
                    .name(wireEventName(event.type()))
                    .data(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize fiscalizacion SSE event for mac={}: {}", event.mac(), ex.getMessage());
            return null;
        }
    }

    private void sendToKey(String key, SseEmitter.SseEventBuilder builder) {
        Set<SseEmitter> emitters = emittersByKey.get(key);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(builder);
            } catch (IOException | IllegalStateException ex) {
                unsubscribe(key, emitter);
            }
        }
    }

    private void unsubscribe(String key, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByKey.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByKey.remove(key, emitters);
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // already closed
        }
    }

    private static String macKey(String compactMac) {
        return MAC_PREFIX + (compactMac == null ? "" : compactMac.trim().toUpperCase(Locale.ROOT));
    }

    private static String regKey(String ptrReg) {
        return REG_PREFIX + (ptrReg == null ? "" : ptrReg.trim().toUpperCase(Locale.ROOT));
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
