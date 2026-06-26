package com.aeg.core.mqtt;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.enajenacion.mqtt.EnajenacionAlreadyCompletedException;
import com.aeg.core.enajenacion.mqtt.EnajenacionPreconditionValidator;
import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionRegistry;
import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityDirection;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityQuery;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityStore;
import com.aeg.core.mqtt.dto.EnajenacionActiveSessionResponse;
import com.aeg.core.mqtt.dto.EnajenacionActivityEntryResponse;
import com.aeg.core.mqtt.dto.EnajenacionActivityListResponse;
import com.aeg.core.mqtt.dto.EnajenacionMqttPrecheckResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/enajenacion")
@RequiredArgsConstructor
public class EnajenacionMqttAdminController {

    private final EnajenacionPreconditionValidator preconditionValidator;
    private final EnajenacionActivityStore activityStore;
    private final EnajenacionSessionRegistry sessionRegistry;

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

    @GetMapping("/activity")
    public EnajenacionActivityListResponse activity(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String mac,
            @RequestParam(required = false) String ptrReg,
            @RequestParam(required = false) EnajenacionActivityResult result,
            @RequestParam(required = false) EnajenacionActivityDirection direction,
            @RequestParam(defaultValue = "false") boolean sessionOnly) {
        EnajenacionActivityQuery query = new EnajenacionActivityQuery(
                mac,
                result,
                ptrReg,
                direction,
                sessionOnly);
        long total = activityStore.count(query);
        List<EnajenacionActivityEntryResponse> entries = activityStore.find(query, limit, page).stream()
                .map(EnajenacionActivityEntryResponse::from)
                .toList();
        return new EnajenacionActivityListResponse(entries, Math.toIntExact(total));
    }

    @GetMapping("/sessions")
    public List<EnajenacionActiveSessionResponse> sessions() {
        return sessionRegistry.listActive().stream()
                .map(EnajenacionActiveSessionResponse::from)
                .toList();
    }
}
