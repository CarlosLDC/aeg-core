package com.aeg.core.enajenacion.mqtt.sse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.security.AdminJwtTokenValidator;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mqtt/enajenacion")
@RequiredArgsConstructor
public class EnajenacionSseController {

    private final AdminJwtTokenValidator adminJwtTokenValidator;
    private final EnajenacionSseBroadcaster broadcaster;
    private final EnajenacionSseNotifier notifier;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> stream(
            @RequestParam String mac,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String jwt = AdminJwtTokenValidator.extractTokenFromQueryOrHeader(
                token,
                authorization == null ? null : java.util.List.of(authorization));
        if (adminJwtTokenValidator.validateAdminToken(jwt).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (mac == null || mac.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String compactMac = MacAddressNormalizer.toCompactForm(mac);
        SseEmitter emitter = broadcaster.subscribe(compactMac);
        try {
            notifier.notifyConnected(compactMac);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache")
                .body(emitter);
    }
}
