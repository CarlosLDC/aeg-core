package com.aeg.core.mqtt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.enajenacion.mqtt.EnajenacionAlreadyCompletedException;
import com.aeg.core.enajenacion.mqtt.EnajenacionPreconditionValidator;
import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.mqtt.dto.EnajenacionMqttPrecheckResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/enajenacion")
@RequiredArgsConstructor
public class EnajenacionMqttAdminController {

    private final EnajenacionPreconditionValidator preconditionValidator;

    @GetMapping("/precheck")
    public EnajenacionMqttPrecheckResponse precheck(
            @RequestParam String ptrReg,
            @RequestParam String mac) {
        String compactMac = MacAddressNormalizer.toCompactForm(mac);
        try {
            preconditionValidator.validateAndBuildContext(ptrReg, compactMac, mac);
            return new EnajenacionMqttPrecheckResponse(true, null);
        } catch (EnajenacionAlreadyCompletedException ex) {
            return new EnajenacionMqttPrecheckResponse(false, "La impresora ya está enajenada.");
        } catch (EnajenacionProtocolException ex) {
            return new EnajenacionMqttPrecheckResponse(false, ex.getMessage());
        }
    }
}
