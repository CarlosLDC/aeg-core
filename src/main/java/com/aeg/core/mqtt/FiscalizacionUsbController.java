package com.aeg.core.mqtt;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionPreconditionValidator;
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerConfigResult;
import com.aeg.core.mqtt.dto.FiscalizacionUsbBrokerConfigResponse;
import com.aeg.core.mqtt.dto.FiscalizacionUsbConfigureBrokerRequest;
import com.aeg.core.mqtt.dto.FiscalizacionUsbPrecheckRequest;
import com.aeg.core.mqtt.dto.FiscalizacionUsbPrecheckResponse;
import com.aeg.core.tools.mqtt.BrokerMigrationService;
import com.aeg.core.tools.mqtt.ToolsMqttConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

/**
 * Soporte backend del asistente de fiscalización por USB (impresoras nuevas). La comunicación con la
 * impresora ocurre en el navegador vía Web Serial; aquí solo se valida, se entrega la configuración
 * del broker nuevo y se ofrece el fallback de cambio de broker vía broker antiguo.
 */
@RestController
@RequestMapping("/api/mqtt/fiscalizacion/usb")
public class FiscalizacionUsbController {

    private final FiscalizacionPreconditionValidator preconditionValidator;
    private final BrokerMigrationService brokerMigrationService;
    private final ObjectMapper objectMapper;

    public FiscalizacionUsbController(
            FiscalizacionPreconditionValidator preconditionValidator,
            BrokerMigrationService brokerMigrationService,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.preconditionValidator = preconditionValidator;
        this.brokerMigrationService = brokerMigrationService;
        this.objectMapper = objectMapper;
    }

    /** Valida serial, precinto/color y MAC (opcional) antes del paso irreversible. */
    @PostMapping("/precheck")
    public FiscalizacionUsbPrecheckResponse precheck(@Valid @RequestBody FiscalizacionUsbPrecheckRequest request) {
        List<String> errors = preconditionValidator.precheck(
                request.ptrReg(), request.precintoNro(), request.precintoColor(), request.mac());
        return new FiscalizacionUsbPrecheckResponse(errors.isEmpty(), errors);
    }

    /** Payload {@code configMqttAEG} para enviarlo por USB (las credenciales no viven en el frontend). */
    @GetMapping("/broker-config")
    public FiscalizacionUsbBrokerConfigResponse brokerConfig() throws Exception {
        return new FiscalizacionUsbBrokerConfigResponse(
                objectMapper.readTree(brokerMigrationService.configMqttPayload()),
                ToolsMqttConstants.NEW_BROKER_HOST,
                ToolsMqttConstants.LEGACY_BROKER_HOST);
    }

    /** Fallback: envía {@code configMqttAEG} por el broker antiguo a una impresora aún no registrada. */
    @PostMapping("/configure-broker")
    public ResponseEntity<BrokerMigrationBrokerConfigResult> configureBroker(
            @Valid @RequestBody FiscalizacionUsbConfigureBrokerRequest request) {
        return ResponseEntity.ok(brokerMigrationService.configureBrokerByMac(request.mac()));
    }
}
