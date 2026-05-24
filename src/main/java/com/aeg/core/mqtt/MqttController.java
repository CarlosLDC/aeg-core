package com.aeg.core.mqtt;

import com.aeg.core.mqtt.dto.MqttPublishRequest;
import com.aeg.core.mqtt.dto.MqttPublishResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.mqtt.dto.MqttMonitorStatusResponse;
import com.aeg.core.mqtt.dto.MqttSubscriptionRequest;
import com.aeg.core.mqtt.dto.MqttSubscriptionResponse;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    private final MqttService mqttService;
    private final MqttConnectionProbeService mqttConnectionProbeService;
    private final MqttMonitorStatusService mqttMonitorStatusService;
    private final MqttMessageHistory mqttMessageHistory;
    private final MqttSubscriptionManager mqttSubscriptionManager;
    private final ObjectMapper objectMapper;

    public MqttController(
            MqttService mqttService,
            MqttConnectionProbeService mqttConnectionProbeService,
            MqttMonitorStatusService mqttMonitorStatusService,
            MqttMessageHistory mqttMessageHistory,
            MqttSubscriptionManager mqttSubscriptionManager,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.mqttService = mqttService;
        this.mqttConnectionProbeService = mqttConnectionProbeService;
        this.mqttMonitorStatusService = mqttMonitorStatusService;
        this.mqttMessageHistory = mqttMessageHistory;
        this.mqttSubscriptionManager = mqttSubscriptionManager;
        this.objectMapper = objectMapper;
    }

    @Value("${app.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @PostMapping("/test")
    public Map<String, String> testConnection() {
        mqttService.sendTestMessage();
        return Map.of(
            "status", "sent",
            "message", "Test message sent to MQTT. Check logs for connection result.",
            "broker", brokerUrl
        );
    }

    @PostMapping("/publish")
    public ResponseEntity<MqttPublishResponse> publish(@Valid @RequestBody MqttPublishRequest request) {
        Object payload = request.payload();
        if (MqttPayloads.isEmpty(payload)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "payload must be a non-empty JSON object or a non-empty JSON array");
        }

        final String serializedPayload;

        try {
            serializedPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload could not be serialized as JSON", ex);
        }

        mqttService.publish(request.topic(), serializedPayload);

        MqttPublishResponse response = new MqttPublishResponse(
            "sent",
            request.topic(),
            payload,
            brokerUrl
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/connection-check")
    public ResponseEntity<MqttConnectionProbeResult> connectionCheck() {
        MqttConnectionProbeResult result = mqttConnectionProbeService.probe();
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }

    @GetMapping("/status")
    public MqttMonitorStatusResponse monitorStatus() {
        return mqttMonitorStatusService.status();
    }

    @GetMapping("/subscription")
    public MqttSubscriptionResponse getSubscription() {
        return mqttSubscriptionManager.current();
    }

    @PutMapping("/subscription")
    public MqttSubscriptionResponse updateSubscription(@Valid @RequestBody MqttSubscriptionRequest request) {
        try {
            return mqttSubscriptionManager.updateTopic(request.topic());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/messages")
    public List<MqttInboundMessage> recentMessages(
            @RequestParam(defaultValue = "50") int limit) {
        return mqttMessageHistory.recent(limit);
    }
}
