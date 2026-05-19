package com.aeg.core.mqtt;

import com.aeg.core.mqtt.dto.MqttPublishRequest;
import com.aeg.core.mqtt.dto.MqttPublishResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@RestController
@RequestMapping("/api/mqtt")
@RequiredArgsConstructor
public class MqttController {

    private final MqttService mqttService;
    private final MqttConnectionProbeService mqttConnectionProbeService;
    private final ObjectMapper objectMapper;

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
        JsonNode payloadNode = request.payload();
        if (payloadNode == null || payloadNode.isNull() || isEmptyPayload(payloadNode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "payload must be a non-empty JSON object or a non-empty JSON array");
        }

        final String serializedPayload;

        try {
            serializedPayload = objectMapper.writeValueAsString(payloadNode);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload could not be serialized as JSON", ex);
        }

        mqttService.publish(request.topic(), serializedPayload);

        MqttPublishResponse response = new MqttPublishResponse(
            "sent",
            request.topic(),
            request.payload(),
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

    private static boolean isEmptyPayload(JsonNode payload) {
        if (payload.isObject() || payload.isArray()) {
            return payload.isEmpty();
        }
        return true;
    }
}
