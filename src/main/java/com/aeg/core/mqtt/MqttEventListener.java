package com.aeg.core.mqtt;

import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MqttEventListener {

    @EventListener
    public void handleMqttEvent(MqttIntegrationEvent event) {
        if (event instanceof MqttConnectionFailedEvent) {
            log.error("❌ MQTT Connection Failed: {}", event.getCause().getMessage());
        } else if (event instanceof MqttSubscribedEvent) {
            log.info("✅ MQTT Subscribed successfully");
        } else if (event instanceof MqttMessageSentEvent) {
            log.info("🚀 MQTT Message sent successfully");
        } else {
            log.info("ℹ️ MQTT Event: {}", event.getClass().getSimpleName());
        }
    }
}
