package com.aeg.core.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Configuration
@org.springframework.integration.annotation.IntegrationComponentScan
@lombok.extern.slf4j.Slf4j
public class MqttConfig {

    @Value("${app.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${app.mqtt.client-id:aeg-core-server}")
    private String clientIdBase;

    @Value("${app.mqtt.inbound.topic:aeg/telemetry/#}")
    private String inboundTopic;

    @Value("${app.mqtt.inbound.enabled:true}")
    private boolean inboundEnabled;

    @Value("${app.mqtt.default-topic:aeg/commands}")
    private String defaultTopic;

    @Value("${app.mqtt.username:}")
    private String username;

    @Value("${app.mqtt.password:}")
    private String password;

    @PostConstruct
    void logMqttSettings() {
        log.info(
                "MQTT broker={}, outboundClientId={}, inboundEnabled={}",
                brokerUrl,
                MqttClientIds.clientId(clientIdBase) + "-out",
                inboundEnabled);
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(45);
        options.setMaxReconnectDelay(15_000);

        if (username != null && !username.isBlank()) {
            options.setUserName(username);
        }
        if (password != null && !password.isBlank()) {
            options.setPassword(password.toCharArray());
        }

        return options;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions());
        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                MqttClientIds.clientId(clientIdBase) + "-out",
                mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(defaultTopic);
        return messageHandler;
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface MqttGateway {
        void sendToMqtt(String data);
        void sendToMqtt(String data, @Header(MqttHeaders.TOPIC) String topic);
    }

    // --- Inbound Configuration (Subscriptions) ---

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ConditionalOnProperty(name = "app.mqtt.inbound.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter mqttInbound() {
        org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter adapter =
                new org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter(
                        MqttClientIds.clientId(clientIdBase) + "-in",
                        mqttClientFactory(),
                        inboundTopic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new org.springframework.integration.mqtt.support.DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());
        log.info("MQTT inbound enabled (topic={}, clientSuffix={})", inboundTopic, MqttClientIds.suffix());
        return adapter;
    }

    @Bean
    @ConditionalOnProperty(name = "app.mqtt.inbound.enabled", havingValue = "true", matchIfMissing = true)
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler handler() {
        return message -> {
            Object payload = message.getPayload();
            Object topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
            log.info("📥 MQTT Message received on topic {}: {}", topic, payload);
        };
    }
}
