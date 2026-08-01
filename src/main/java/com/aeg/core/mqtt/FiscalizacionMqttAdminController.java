package com.aeg.core.mqtt;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionRegistry;
import com.aeg.core.fiscalizacion.mqtt.activity.FiscalizacionActivityStore;
import com.aeg.core.mqtt.dto.FiscalizacionActiveSessionResponse;
import com.aeg.core.mqtt.dto.FiscalizacionActivityEntryResponse;
import com.aeg.core.mqtt.dto.FiscalizacionActivityListResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/fiscalizacion")
@RequiredArgsConstructor
public class FiscalizacionMqttAdminController {

    private final FiscalizacionActivityStore activityStore;
    private final FiscalizacionSessionRegistry sessionRegistry;

    @GetMapping("/activity")
    public FiscalizacionActivityListResponse activity(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String mac) {
        List<FiscalizacionActivityEntryResponse> entries = activityStore.find(mac, limit).stream()
                .map(FiscalizacionActivityEntryResponse::from)
                .toList();
        return new FiscalizacionActivityListResponse(entries, entries.size());
    }

    @GetMapping("/sessions")
    public List<FiscalizacionActiveSessionResponse> sessions() {
        return sessionRegistry.listActive().stream()
                .map(FiscalizacionActiveSessionResponse::from)
                .toList();
    }
}
