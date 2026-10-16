package com.aeg.core.tools.mqtt;

import java.nio.charset.StandardCharsets;
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
import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente MQTT efímero contra el broker legacy (anónimo, sin credenciales) usado
 * para migración de broker y consulta de estado legacy.
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
            Predicate<FiscalMqttResponseItem> itemMatcher,
            int timeoutSeconds) {
        return publishAndAwaitExtract(
                compactMac,
                payload,
                raw -> extractMatchingResponseItem(raw, itemMatcher),
                timeoutSeconds);
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
        return cmd != null && cmd.isTextual() && cmd.asText().trim().equalsIgnoreCase(expectedCmd.trim());
    }

    private <T> T publishAndAwaitExtract(
            String compactMac, String payload, Function<String, T> extractor, int timeoutSeconds) {
        String broker = "tcp://" + ToolsMqttConstants.LEGACY_BROKER_HOST + ":" + ToolsMqttConstants.LEGACY_BROKER_PORT;
        String clientId = "aeg-migr-" + UUID.randomUUID();

        MqttClient client = null;
        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(10);
            connOpts.setAutomaticReconnect(false);

            client.connect(connOpts);
            log.info("Connected to legacy broker at {} with clientId {}", broker, clientId);

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

            client.subscribe(responseTopic, 1);

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
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                    client.close();
                } catch (Exception ex) {
                    log.warn("Failed to close legacy MQTT client", ex);
                }
            }
        }
    }
}
