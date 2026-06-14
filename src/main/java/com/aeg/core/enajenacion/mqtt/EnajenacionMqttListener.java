package com.aeg.core.enajenacion.mqtt;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.aeg.core.mqtt.MqttInboundReceivedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnajenacionMqttListener implements ApplicationListener<MqttInboundReceivedEvent> {

    private final EnajenacionMqttOrchestrator orchestrator;
    private final EnajenacionMqttSettings settings;

    @Override
    public void onApplicationEvent(MqttInboundReceivedEvent event) {
        if (!settings.enabled()) {
            return;
        }
        String topic = event.message().topic();
        if (!FiscalMqttTopics.isFiscalInboundTopic(topic)) {
            return;
        }
        try {
            orchestrator.handleInbound(topic, event.message().payload());
        } catch (RuntimeException ex) {
            log.error("Unhandled enajenacion MQTT error topic={}: {}", topic, ex.getMessage(), ex);
        }
    }
}
