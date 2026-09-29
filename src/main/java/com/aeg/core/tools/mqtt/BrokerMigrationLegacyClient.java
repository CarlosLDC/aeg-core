package com.aeg.core.tools.mqtt;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BrokerMigrationLegacyClient {

    private final ObjectMapper objectMapper;

    public BrokerMigrationLegacyClient(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String publishAndAwait(String compactMac, String payload, String expectedResponseCmd, int timeoutSeconds) {
        return publishAndAwaitMatcher(compactMac, payload, responseJson -> {
            try {
                JsonNode node = objectMapper.readTree(responseJson);
                if (node.has("cmd") && node.get("cmd").isTextual()) {
                    return expectedResponseCmd.equals(node.get("cmd").asText());
                }
            } catch (Exception e) {
                log.warn("Error parsing response JSON for match", e);
            }
            return false;
        }, timeoutSeconds);
    }

    public String publishAndAwaitMatcher(String compactMac, String payload, Predicate<String> responseMatcher, int timeoutSeconds) {
        String broker = "tcp://" + ToolsMqttConstants.LEGACY_BROKER_HOST + ":" + ToolsMqttConstants.LEGACY_BROKER_PORT;
        String clientId = UUID.randomUUID().toString();
        
        MqttClient client = null;
        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(10);
            
            client.connect(connOpts);
            log.info("Connected to legacy broker at {} with clientId {}", broker, clientId);
            
            String responseTopic = FiscalMqttTopics.respuestaTopic(compactMac);
            String commandTopic = FiscalMqttTopics.comandoTopic(compactMac);
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> matchedResponse = new AtomicReference<>();
            
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection lost to legacy broker for clientId {}", clientId, cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());
                    log.info("Received message on topic {}: {}", topic, msg);
                    if (responseMatcher.test(msg)) {
                        matchedResponse.set(msg);
                        latch.countDown();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            
            client.subscribe(responseTopic);
            
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(0);
            
            log.info("Publishing payload to legacy broker topic {}: {}", commandTopic, payload);
            client.publish(commandTopic, message);
            
            boolean received = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!received) {
                throw new EnajenacionProtocolException("Timeout waiting for expected response on legacy broker.");
            }
            
            return matchedResponse.get();
        } catch (EnajenacionProtocolException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with legacy broker", e);
            throw new EnajenacionProtocolException("Error communicating with legacy broker: " + e.getMessage());
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
