package com.aeg.core.mqtt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.mqtt.dto.BrokerMigrationBrokerConfigResult;
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerResult;
import com.aeg.core.mqtt.dto.BrokerMigrationFirmwareResult;
import com.aeg.core.mqtt.dto.BrokerMigrationRequest;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.tools.mqtt.BrokerMigrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/tools/broker-migration")
@RequiredArgsConstructor
public class BrokerMigrationMqttController {

    private final BrokerMigrationService brokerMigrationService;

    @PostMapping("/check-firmware")
    public ResponseEntity<BrokerMigrationFirmwareResult> checkFirmware(@Valid @RequestBody BrokerMigrationRequest request) {
        return ResponseEntity.ok(brokerMigrationService.checkFirmware(request.printerId()));
    }

    @PostMapping("/update-firmware")
    public ResponseEntity<ToolsMqttSimpleResponse> updateFirmware(@Valid @RequestBody BrokerMigrationRequest request) {
        return ResponseEntity.ok(brokerMigrationService.triggerFirmwareUpdate(request.printerId()));
    }

    @PostMapping("/check-broker")
    public ResponseEntity<BrokerMigrationBrokerResult> checkBroker(@Valid @RequestBody BrokerMigrationRequest request) {
        return ResponseEntity.ok(brokerMigrationService.checkBroker(request.printerId()));
    }

    @PostMapping("/configure-broker")
    public ResponseEntity<BrokerMigrationBrokerConfigResult> configureBroker(@Valid @RequestBody BrokerMigrationRequest request) {
        return ResponseEntity.ok(brokerMigrationService.configureBroker(request.printerId()));
    }
}
