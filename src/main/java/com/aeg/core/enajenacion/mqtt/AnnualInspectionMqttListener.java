package com.aeg.core.enajenacion.mqtt;

import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.aeg.core.mqtt.MqttInboundReceivedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AnnualInspectionMqttListener implements ApplicationListener<MqttInboundReceivedEvent> {

    private final FiscalMqttSyncResponseAwaiter syncResponseAwaiter;

    @Override
    public void onApplicationEvent(MqttInboundReceivedEvent event) {
        String topic = event.message().topic();
        if (!FiscalMqttTopics.isFiscalInboundTopic(topic)) {
            return;
        }
        FiscalMqttTopics.extractCompactMac(topic).ifPresent(mac ->
                syncResponseAwaiter.tryComplete(mac, topic, event.message().payload()));
    }
}
