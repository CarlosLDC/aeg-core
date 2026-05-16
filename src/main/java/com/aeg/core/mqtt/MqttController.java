package com.aeg.core.mqtt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@RestController
@RequestMapping("/api/mqtt")
@RequiredArgsConstructor
public class MqttController {

    private final MqttService mqttService;
    private final MqttConnectionProbeService mqttConnectionProbeService;

    @Value("${app.mqtt.broker-url}")
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

    @GetMapping("/connection-check")
    public ResponseEntity<MqttConnectionProbeResult> connectionCheck() {
        MqttConnectionProbeResult result = mqttConnectionProbeService.probe();
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }
}
