package com.aeg.core.mqtt;

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

    @PostMapping("/test")
    public Map<String, String> testConnection() {
        mqttService.sendTestMessage();
        return Map.of(
            "status", "sent",
            "message", "Test message sent to MQTT. Check logs for connection result.",
            "broker", "10.116.0.4"
        );
    }
}
