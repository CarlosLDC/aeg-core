package com.aeg.core.mqtt;

import org.springframework.stereotype.Service;
import com.aeg.core.config.MqttConfig.MqttGateway;
import com.aeg.core.fiscal.FiscalTicketLatin2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttService {

    private final MqttGateway mqttGateway;

    public void publish(String topic, String payload) {
        log.info("Publishing to MQTT topic {}: {}", topic, payload);
        mqttGateway.sendToMqtt(FiscalTicketLatin2.encodeMqttPayload(topic, payload), topic);
    }

    public void sendTestMessage() {
        String topic = "aeg/test";
        String payload = "Connection test from AEG Core at " + java.time.OffsetDateTime.now();
        log.info("Sending test MQTT message to {}", topic);
        mqttGateway.sendToMqtt(FiscalTicketLatin2.encodeMqttPayload(topic, payload), topic);
    }
}
