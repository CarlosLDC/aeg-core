package com.aeg.core.enajenacion.mqtt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FiscalMqttSyncResponseAwaiter {

    private enum WaitMode {
        OBJECT,
        ARRAY_TERMINAL,
        MATCHER,
        TEXT_CHUNKS
    }

    public record ToolsMqttTextChunksResult(List<String> chunks, FiscalMqttResponseItem terminal) {}

    private record PendingWait(
            CompletableFuture<?> future,
            WaitMode mode,
            String expectedCmd,
            Predicate<FiscalMqttResponseItem> matcher,
            List<String> textChunks) {}

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, PendingWait> pendingByMac = new ConcurrentHashMap<>();

    public FiscalMqttSyncResponseAwaiter(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<FiscalMqttResponseItem> register(String compactMac, String expectedCmd) {
        CompletableFuture<FiscalMqttResponseItem> future = new CompletableFuture<>();
        pendingByMac.put(compactMac, new PendingWait(future, WaitMode.OBJECT, expectedCmd, null, null));
        return future;
    }

    public CompletableFuture<FiscalMqttResponseItem> registerWithMatcher(
            String compactMac,
            Predicate<FiscalMqttResponseItem> matcher) {
        CompletableFuture<FiscalMqttResponseItem> future = new CompletableFuture<>();
        pendingByMac.put(compactMac, new PendingWait(future, WaitMode.MATCHER, null, matcher, null));
        return future;
    }

    public CompletableFuture<ToolsMqttTextChunksResult> registerTextChunks(
            String compactMac,
            String terminalCmd) {
        CompletableFuture<ToolsMqttTextChunksResult> future = new CompletableFuture<>();
        pendingByMac.put(
                compactMac,
                new PendingWait(future, WaitMode.TEXT_CHUNKS, terminalCmd, null, new ArrayList<>()));
        return future;
    }

    public CompletableFuture<List<FiscalMqttResponseItem>> registerArrayTerminal(
            String compactMac,
            String terminalCmd) {
        CompletableFuture<List<FiscalMqttResponseItem>> future = new CompletableFuture<>();
        pendingByMac.put(compactMac, new PendingWait(future, WaitMode.ARRAY_TERMINAL, terminalCmd, null, null));
        return future;
    }

    public void cancel(String compactMac) {
        PendingWait removed = pendingByMac.remove(compactMac);
        if (removed != null) {
            removed.future().cancel(false);
        }
    }

    /**
     * @return true if a pending wait was completed with this inbound message
     */
    public boolean tryComplete(String compactMac, String topic, String payload) {
        PendingWait wait = pendingByMac.get(compactMac);
        if (wait == null || !FiscalMqttTopics.isRespuestaTopic(topic)) {
            return false;
        }
        return switch (wait.mode()) {
            case OBJECT -> tryCompleteObjectWait(compactMac, wait, payload);
            case ARRAY_TERMINAL -> tryCompleteArrayWait(compactMac, wait, payload);
            case MATCHER -> tryCompleteMatcherWait(compactMac, wait, payload);
            case TEXT_CHUNKS -> tryCompleteTextChunksWait(compactMac, wait, payload);
        };
    }

    private boolean tryCompleteObjectWait(String compactMac, PendingWait wait, String payload) {
        FiscalMqttResponseItem item = tryParseObjectResponse(payload).orElse(null);
        if (item == null || !cmdEquals(item.cmd(), wait.expectedCmd())) {
            return false;
        }
        pendingByMac.remove(compactMac);
        completeFuture(wait.future(), item);
        log.debug("Completed sync MQTT object wait mac={} cmd={}", compactMac, wait.expectedCmd());
        return true;
    }

    private boolean tryCompleteMatcherWait(String compactMac, PendingWait wait, String payload) {
        FiscalMqttResponseItem item = tryParseObjectResponse(payload).orElse(null);
        if (item == null || wait.matcher() == null || !wait.matcher().test(item)) {
            return false;
        }
        pendingByMac.remove(compactMac);
        completeFuture(wait.future(), item);
        log.debug("Completed sync MQTT matcher wait mac={}", compactMac);
        return true;
    }

    private boolean tryCompleteTextChunksWait(String compactMac, PendingWait wait, String payload) {
        FiscalMqttResponseItem item = tryParseObjectResponse(payload).orElse(null);
        if (item != null) {
            if (!cmdEquals(item.cmd(), wait.expectedCmd())) {
                return false;
            }
            pendingByMac.remove(compactMac);
            completeFuture(wait.future(), new ToolsMqttTextChunksResult(List.copyOf(wait.textChunks()), item));
            log.debug("Completed sync MQTT text-chunks wait mac={} cmd={}", compactMac, wait.expectedCmd());
            return true;
        }
        if (payload != null && !payload.isBlank()) {
            wait.textChunks().add(payload);
            return true;
        }
        return false;
    }

    private boolean tryCompleteArrayWait(String compactMac, PendingWait wait, String payload) {
        List<FiscalMqttResponseItem> items = tryParseArrayResponse(payload).orElse(null);
        if (items == null || items.isEmpty()) {
            return false;
        }
        boolean hasTerminal = items.stream().anyMatch(item -> cmdEquals(item.cmd(), wait.expectedCmd()));
        if (!hasTerminal) {
            return false;
        }
        pendingByMac.remove(compactMac);
        completeFuture(wait.future(), items);
        log.debug("Completed sync MQTT array wait mac={} terminalCmd={}", compactMac, wait.expectedCmd());
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> void completeFuture(CompletableFuture<?> future, T value) {
        ((CompletableFuture<T>) future).complete(value);
    }

    private static boolean cmdEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.trim().equalsIgnoreCase(expected.trim());
    }

    private java.util.Optional<FiscalMqttResponseItem> tryParseObjectResponse(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (!node.isObject()) {
                return java.util.Optional.empty();
            }
            if (node.has("dataS") && !node.get("dataS").isTextual()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node)
                        .put("dataS", objectMapper.writeValueAsString(node.get("dataS")));
            }
            return java.util.Optional.of(objectMapper.treeToValue(node, FiscalMqttResponseItem.class));
        } catch (IOException ex) {
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<List<FiscalMqttResponseItem>> tryParseArrayResponse(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (!node.isArray()) {
                return java.util.Optional.empty();
            }
            FiscalMqttResponseItem[] items = objectMapper.treeToValue(node, FiscalMqttResponseItem[].class);
            return java.util.Optional.of(Arrays.asList(items));
        } catch (IOException ex) {
            return java.util.Optional.empty();
        }
    }
}
