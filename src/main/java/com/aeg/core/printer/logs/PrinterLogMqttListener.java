package com.aeg.core.printer.logs;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.mqtt.MqttInboundMessage;
import com.aeg.core.mqtt.MqttInboundReceivedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Listens for MQTT inbound messages and routes printer logs (DebugMQTT)
 * into the asynchronous batch ingestion service.
 */
@Component
@Slf4j
public class PrinterLogMqttListener implements ApplicationListener<MqttInboundReceivedEvent> {

    private final PrinterLogIngestionService ingestionService;

    @Value("${app.mqtt.logs.enabled:true}")
    private boolean logsEnabled;

    @Value("${app.mqtt.logs.inbound-topic:+/AEG_Fiscal/Integracion/DebugMQTT}")
    private String logsInboundTopic;

    public PrinterLogMqttListener(PrinterLogIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void onApplicationEvent(MqttInboundReceivedEvent event) {
        if (!logsEnabled) {
            return;
        }

        MqttInboundMessage message = event.message();
        if (message == null || message.topic() == null) {
            return;
        }

        String topic = message.topic().trim();
        if (!FiscalMqttTopics.isDebugMqttTopic(topic) && !matchesConfiguredTopic(topic)) {
            return;
        }

        Optional<String> compactMac = FiscalMqttTopics.extractCompactMac(topic);
        String mac = compactMac.orElse("UNKNOWN");

        PrinterLogEntry entry = new PrinterLogEntry(
                mac,
                topic,
                message.payload() != null ? message.payload() : "",
                message.receivedAt() != null ? message.receivedAt() : java.time.Instant.now()
        );

        ingestionService.enqueue(entry);
    }

    private boolean matchesConfiguredTopic(String topic) {
        if (logsInboundTopic == null || logsInboundTopic.isBlank()) {
            return false;
        }
        // Basic match for wildcard or direct topic
        if (logsInboundTopic.endsWith("/#")) {
            String prefix = logsInboundTopic.substring(0, logsInboundTopic.length() - 2);
            return topic.startsWith(prefix) || topic.startsWith("/" + prefix);
        }
        return false;
    }
}
