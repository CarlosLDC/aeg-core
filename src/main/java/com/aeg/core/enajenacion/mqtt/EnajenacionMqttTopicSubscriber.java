package com.aeg.core.enajenacion.mqtt;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(name = "app.mqtt.inbound.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class EnajenacionMqttTopicSubscriber {

    private static final int SUBSCRIPTION_QOS = 1;

    private static final String RESPUESTA_TOPIC = "+/AEG_Fiscal/Integracion/Respuesta";

    private final EnajenacionMqttSettings settings;
    private final ObjectProvider<MqttPahoMessageDrivenChannelAdapter> inboundAdapter;

    public EnajenacionMqttTopicSubscriber(
            EnajenacionMqttSettings settings,
            ObjectProvider<MqttPahoMessageDrivenChannelAdapter> inboundAdapter) {
        this.settings = settings;
        this.inboundAdapter = inboundAdapter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeAfterReady() {
        if (!settings.enabled()) {
            return;
        }
        MqttPahoMessageDrivenChannelAdapter adapter = inboundAdapter.getIfAvailable();
        if (adapter == null) {
            log.warn("Enajenacion MQTT topic not subscribed: inbound adapter unavailable");
            return;
        }
        Thread starter = new Thread(() -> subscribeWhenRunning(adapter), "enajenacion-mqtt-subscribe");
        starter.setDaemon(true);
        starter.start();
    }

    private void subscribeWhenRunning(MqttPahoMessageDrivenChannelAdapter adapter) {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (adapter.isRunning()) {
                adapter.addTopic(settings.inboundTopic(), SUBSCRIPTION_QOS);
                adapter.addTopic(RESPUESTA_TOPIC, SUBSCRIPTION_QOS);
                log.info(
                        "Enajenacion MQTT subscribed to topics {} and {}",
                        settings.inboundTopic(),
                        RESPUESTA_TOPIC);
                return;
            }
            try {
                Thread.sleep(1_000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Enajenacion MQTT subscription skipped: inbound adapter did not start in time");
    }
}
