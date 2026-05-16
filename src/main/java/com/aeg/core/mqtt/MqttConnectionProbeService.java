package com.aeg.core.mqtt;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MqttConnectionProbeService {

    @Value("${app.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${app.mqtt.client-id:aeg-core-server}")
    private String clientId;

    @Value("${app.mqtt.username:}")
    private String username;

    @Value("${app.mqtt.password:}")
    private String password;

    public MqttConnectionProbeResult probe() {
        Instant startedAt = Instant.now();
        MqttClient client = null;

        try {
            client = new MqttClient(brokerUrl, clientId + "-probe-" + UUID.randomUUID(), new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setServerURIs(new String[] { brokerUrl });
            options.setCleanSession(true);
            options.setAutomaticReconnect(false);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(10);

            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }
            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }

            client.connect(options);

            return new MqttConnectionProbeResult(
                    true,
                    client.isConnected(),
                    brokerUrl,
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    "Broker connection successful"
            );
        } catch (MqttException ex) {
            log.warn("MQTT connection probe failed for {}: {}", brokerUrl, ex.getMessage());
            return new MqttConnectionProbeResult(
                    false,
                    false,
                    brokerUrl,
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    ex.getMessage()
            );
        } finally {
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                } catch (MqttException ex) {
                    log.debug("Error disconnecting MQTT probe client: {}", ex.getMessage());
                }

                try {
                    client.close();
                } catch (MqttException ex) {
                    log.debug("Error closing MQTT probe client: {}", ex.getMessage());
                }
            }
        }
    }
}
