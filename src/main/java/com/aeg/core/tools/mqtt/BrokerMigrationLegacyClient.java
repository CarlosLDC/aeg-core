package com.aeg.core.tools.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter.ToolsMqttTextChunksResult;
import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente MQTT efímero contra el broker legacy (anónimo, sin credenciales) usado
 * para migración de broker y como contingencia para operaciones de Tools.
 */
@Slf4j
@Component
public class BrokerMigrationLegacyClient {

    /** La conexión al broker legacy funcionó pero la impresora no respondió a tiempo. */
    public static class LegacyBrokerTimeoutException extends EnajenacionProtocolException {
        public LegacyBrokerTimeoutException(String message) {
            super(message);
        }
    }

    static final class LegacyTextChunksState {
        private final String expectedCmd;
        private final List<String> textChunks = new ArrayList<>();
        private FiscalMqttResponseItem terminalAck;
        private boolean documentoSeen;

        LegacyTextChunksState(String expectedCmd) {
            this.expectedCmd = expectedCmd;
        }
    }

    private final ObjectMapper objectMapper;

    public BrokerMigrationLegacyClient(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Publica {@code payload} en el topic Comando de la impresora y espera la respuesta cuyo
     * {@code cmd} coincida (sin distinguir mayúsculas). Acepta respuestas en objeto o en array;
     * en el caso de array devuelve el JSON del elemento que coincide.
     */
    public String publishAndAwait(String compactMac, String payload, String expectedResponseCmd, int timeoutSeconds) {
        return publishAndAwaitExtract(compactMac, payload, raw -> extractMatchingItem(raw, expectedResponseCmd), timeoutSeconds);
    }

    public String publishAndAwaitMatcher(String compactMac, String payload, Predicate<String> responseMatcher, int timeoutSeconds) {
        return publishAndAwaitExtract(compactMac, payload, raw -> responseMatcher.test(raw) ? raw : null, timeoutSeconds);
    }

    public FiscalMqttResponseItem publishAndAwaitItem(
            String compactMac,
            String payload,
            String expectedCmd,
            int timeoutSeconds) {
        return publishAndAwaitItem(
                compactMac,
                payload,
                item -> cmdMatches(item.cmd(), expectedCmd),
                timeoutSeconds);
    }

    public FiscalMqttResponseItem publishAndAwaitItem(
            String compactMac,
            String payload,
            Predicate<FiscalMqttResponseItem> itemMatcher,
            int timeoutSeconds) {
        return publishAndAwaitExtract(
                compactMac,
                payload,
                raw -> extractMatchingResponseItem(raw, itemMatcher),
                timeoutSeconds);
    }

    public FiscalMqttResponseItem publishSequenceAndAwaitItem(
            String compactMac,
            List<String> payloads,
            int commandDelayMs,
            String terminalCmd,
            int timeoutSeconds) {
        String broker = "tcp://" + ToolsMqttConstants.LEGACY_BROKER_HOST + ":" + ToolsMqttConstants.LEGACY_BROKER_PORT;
        String clientId = "aeg-migr-" + UUID.randomUUID();

        MqttClient client = null;
        try {
            client = connectClient(broker, clientId);

            String responseTopic = FiscalMqttTopics.respuestaTopic(compactMac);
            String commandTopic = FiscalMqttTopics.comandoTopic(compactMac);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<FiscalMqttResponseItem> matchedResponse = new AtomicReference<>();

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection lost to legacy broker for clientId {}", clientId, cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload(), StandardCharsets.UTF_8);
                    log.info("Received message on legacy topic {}: {}", topic, msg);
                    FiscalMqttResponseItem extracted = extractMatchingResponseItem(
                            msg, item -> cmdMatches(item.cmd(), terminalCmd));
                    if (extracted != null && matchedResponse.compareAndSet(null, extracted)) {
                        latch.countDown();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            subscribeWithVariants(client, responseTopic);

            for (int i = 0; i < payloads.size(); i++) {
                String currentPayload = payloads.get(i);
                MqttMessage message = new MqttMessage(currentPayload.getBytes(StandardCharsets.UTF_8));
                message.setQos(0);
                log.info("Publishing sequence payload {}/{} to legacy broker topic {}: {}",
                        i + 1, payloads.size(), commandTopic, currentPayload);
                client.publish(commandTopic, message);
                if (i < payloads.size() - 1 && commandDelayMs > 0) {
                    Thread.sleep(commandDelayMs);
                }
            }

            boolean received = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!received) {
                throw new LegacyBrokerTimeoutException(
                        "La impresora no respondió en el broker antiguo (timeout " + timeoutSeconds + "s).");
            }

            return matchedResponse.get();
        } catch (EnajenacionProtocolException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Operación interrumpida esperando al broker antiguo.");
        } catch (Exception e) {
            log.error("Error communicating with legacy broker", e);
            throw new EnajenacionProtocolException("Error comunicándose con el broker antiguo: " + e.getMessage());
        } finally {
            closeQuietly(client);
        }
    }

    public ToolsMqttTextChunksResult publishAndAwaitTextChunks(
            String compactMac,
            String payload,
            String terminalCmd,
            int timeoutSeconds) {
        String broker = "tcp://" + ToolsMqttConstants.LEGACY_BROKER_HOST + ":" + ToolsMqttConstants.LEGACY_BROKER_PORT;
        String clientId = "aeg-migr-" + UUID.randomUUID();

        MqttClient client = null;
        try {
            client = connectClient(broker, clientId);

            String responseTopic = FiscalMqttTopics.respuestaTopic(compactMac);
            String documentTopic = FiscalMqttTopics.documentoTopic(compactMac);
            String commandTopic = FiscalMqttTopics.comandoTopic(compactMac);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<ToolsMqttTextChunksResult> matchedResult = new AtomicReference<>();
            LegacyTextChunksState state = new LegacyTextChunksState(terminalCmd);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection lost to legacy broker for clientId {}", clientId, cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload(), StandardCharsets.UTF_8);
                    log.info("Received message on legacy topic {}: {}", topic, msg);
                    ToolsMqttTextChunksResult result = processTextChunksMessage(state, topic, msg);
                    if (result != null && matchedResult.compareAndSet(null, result)) {
                        latch.countDown();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            subscribeWithVariants(client, responseTopic, documentTopic);

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(0);

            log.info("Publishing payload to legacy broker topic {}: {}", commandTopic, payload);
            client.publish(commandTopic, message);

            boolean received = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!received) {
                throw new LegacyBrokerTimeoutException(
                        "La impresora no respondió en el broker antiguo (timeout " + timeoutSeconds + "s).");
            }

            return matchedResult.get();
        } catch (EnajenacionProtocolException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Operación interrumpida esperando al broker antiguo.");
        } catch (Exception e) {
            log.error("Error communicating with legacy broker", e);
            throw new EnajenacionProtocolException("Error comunicándose con el broker antiguo: " + e.getMessage());
        } finally {
            closeQuietly(client);
        }
    }

    public void publishFireAndForget(String compactMac, String payload) {
        String broker = "tcp://" + ToolsMqttConstants.LEGACY_BROKER_HOST + ":" + ToolsMqttConstants.LEGACY_BROKER_PORT;
        String clientId = "aeg-migr-" + UUID.randomUUID();
        String commandTopic = FiscalMqttTopics.comandoTopic(compactMac);

        MqttClient client = null;
        try {
            client = connectClient(broker, clientId);
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(0);
            log.info("Publishing fire-and-forget payload to legacy broker topic {}: {}", commandTopic, payload);
            client.publish(commandTopic, message);
        } catch (Exception e) {
            log.error("Error publishing fire-and-forget to legacy broker", e);
            throw new EnajenacionProtocolException("Error comunicándose con el broker antiguo: " + e.getMessage());
        } finally {
            closeQuietly(client);
        }
    }

    synchronized ToolsMqttTextChunksResult processTextChunksMessage(
            LegacyTextChunksState state,
            String topic,
            String payload) {
        if (FiscalMqttTopics.isDocumentoTopic(topic)) {
            state.documentoSeen = true;
            String trimmed = payload != null ? payload.trim() : "";
            if ("-1".equals(trimmed)) {
                FiscalMqttResponseItem terminal = state.terminalAck != null
                        ? state.terminalAck
                        : new FiscalMqttResponseItem(state.expectedCmd, 0, 0, null);
                return new ToolsMqttTextChunksResult(List.copyOf(state.textChunks), terminal);
            }
            if (payload != null && !payload.isBlank()) {
                state.textChunks.add(payload + "\n");
            }
            return null;
        }

        if (!FiscalMqttTopics.isRespuestaTopic(topic)) {
            return null;
        }

        if (isJsonCommandPayload(payload)) {
            FiscalMqttResponseItem item = extractMatchingResponseItem(
                    payload, candidate -> cmdMatches(candidate.cmd(), state.expectedCmd));
            if (item == null) {
                return null;
            }
            state.terminalAck = item;
            if (item.code() != null && item.code() != 0) {
                return new ToolsMqttTextChunksResult(List.of(), item);
            }
            if (!state.documentoSeen && !state.textChunks.isEmpty()) {
                return new ToolsMqttTextChunksResult(List.copyOf(state.textChunks), item);
            }
            return null;
        }

        if (payload != null && !payload.isBlank()) {
            state.textChunks.add(payload);
        }
        return null;
    }

    private boolean isJsonCommandPayload(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.isObject()) {
                return node.has("cmd");
            }
            if (node.isArray() && !node.isEmpty()) {
                for (JsonNode element : node) {
                    if (element.isObject() && element.has("cmd")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            // Raw ESC/POS text chunk
        }
        return false;
    }

    FiscalMqttResponseItem extractMatchingResponseItem(String raw, Predicate<FiscalMqttResponseItem> itemMatcher) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isObject()) {
                FiscalMqttResponseItem item = toResponseItem(node);
                return item != null && itemMatcher.test(item) ? item : null;
            }
            if (node.isArray()) {
                for (JsonNode element : node) {
                    if (element.isObject()) {
                        FiscalMqttResponseItem item = toResponseItem(element);
                        if (item != null && itemMatcher.test(item)) {
                            return item;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Ignoring non-JSON message on legacy broker: {}", raw);
        }
        return null;
    }

    private FiscalMqttResponseItem toResponseItem(JsonNode node) throws Exception {
        ObjectNode copy = ((ObjectNode) node).deepCopy();
        if (copy.has("dataS") && !copy.get("dataS").isTextual() && !copy.get("dataS").isNull()) {
            copy.put("dataS", objectMapper.writeValueAsString(copy.get("dataS")));
        }
        return objectMapper.treeToValue(copy, FiscalMqttResponseItem.class);
    }

    private String extractMatchingItem(String raw, String expectedCmd) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isObject()) {
                return cmdMatches(node, expectedCmd) ? raw : null;
            }
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isObject() && cmdMatches(item, expectedCmd)) {
                        return objectMapper.writeValueAsString(item);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Ignoring non-JSON message on legacy broker: {}", raw);
        }
        return null;
    }

    private static boolean cmdMatches(JsonNode node, String expectedCmd) {
        JsonNode cmd = node.get("cmd");
        return cmd != null && cmd.isTextual() && cmdMatches(cmd.asText(), expectedCmd);
    }

    private static boolean cmdMatches(String actualCmd, String expectedCmd) {
        return actualCmd != null
                && expectedCmd != null
                && actualCmd.trim().equalsIgnoreCase(expectedCmd.trim());
    }

    private <T> T publishAndAwaitExtract(
            String compactMac, String payload, Function<String, T> extractor, int timeoutSeconds) {
        String broker = "tcp://" + ToolsMqttConstants.LEGACY_BROKER_HOST + ":" + ToolsMqttConstants.LEGACY_BROKER_PORT;
        String clientId = "aeg-migr-" + UUID.randomUUID();

        MqttClient client = null;
        try {
            client = connectClient(broker, clientId);

            String responseTopic = FiscalMqttTopics.respuestaTopic(compactMac);
            String commandTopic = FiscalMqttTopics.comandoTopic(compactMac);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<T> matchedResponse = new AtomicReference<>();

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection lost to legacy broker for clientId {}", clientId, cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload(), StandardCharsets.UTF_8);
                    log.info("Received message on legacy topic {}: {}", topic, msg);
                    T extracted = extractor.apply(msg);
                    if (extracted != null && matchedResponse.compareAndSet(null, extracted)) {
                        latch.countDown();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            subscribeWithVariants(client, responseTopic);

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(0);

            log.info("Publishing payload to legacy broker topic {}: {}", commandTopic, payload);
            client.publish(commandTopic, message);

            boolean received = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!received) {
                throw new LegacyBrokerTimeoutException(
                        "La impresora no respondió en el broker antiguo (timeout " + timeoutSeconds + "s).");
            }

            return matchedResponse.get();
        } catch (EnajenacionProtocolException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Operación interrumpida esperando al broker antiguo.");
        } catch (Exception e) {
            log.error("Error communicating with legacy broker", e);
            throw new EnajenacionProtocolException("Error comunicándose con el broker antiguo: " + e.getMessage());
        } finally {
            closeQuietly(client);
        }
    }

    private static MqttClient connectClient(String broker, String clientId) throws Exception {
        MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setAutomaticReconnect(false);
        client.connect(connOpts);
        log.info("Connected to legacy broker at {} with clientId {}", broker, clientId);
        return client;
    }

    private static void subscribeWithVariants(MqttClient client, String... canonicalTopics) throws Exception {
        List<String> topics = new ArrayList<>(canonicalTopics.length * 2);
        for (String topic : canonicalTopics) {
            if (topic == null || topic.isBlank()) {
                continue;
            }
            String normalized = topic.trim();
            topics.add(normalized);
            topics.add(normalized.startsWith("/") ? normalized.substring(1) : "/" + normalized);
        }
        int[] qos = new int[topics.size()];
        for (int i = 0; i < qos.length; i++) {
            qos[i] = 1;
        }
        client.subscribe(topics.toArray(String[]::new), qos);
    }

    private static void closeQuietly(MqttClient client) {
        if (client == null) {
            return;
        }
        boolean wasInterrupted = Thread.interrupted();
        try {
            if (client.isConnected()) {
                client.disconnectForcibly(1_000, 1_000);
            }
        } catch (Exception ex) {
            log.debug("Failed to disconnect legacy MQTT client cleanly", ex);
        } finally {
            try {
                client.close(true);
            } catch (Exception ex) {
                log.debug("Failed to close legacy MQTT client", ex);
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
