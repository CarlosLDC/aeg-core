package com.aeg.core.fiscalizacion.mqtt.sse;

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
@RequestMapping("/api/mqtt/fiscalizacion")
@RequiredArgsConstructor
public class FiscalizacionSseController {

    private final AdminJwtTokenValidator adminJwtTokenValidator;
    private final FiscalizacionSseBroadcaster broadcaster;
    private final FiscalizacionSseNotifier notifier;

    /**
     * Stream de eventos de fiscalización. Se identifica por {@code mac} o por {@code ptrReg}
     * (asistente USB, que puede no conocer la MAC antes de que la impresora publique {@code ptrFiscalizar}).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> stream(
            @RequestParam(required = false) String mac,
            @RequestParam(required = false) String ptrReg,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String jwt = AdminJwtTokenValidator.extractTokenFromQueryOrHeader(
                token, authorization == null ? null : java.util.List.of(authorization));
        if (adminJwtTokenValidator.validateAdminToken(jwt).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean hasMac = mac != null && !mac.isBlank();
        boolean hasPtrReg = ptrReg != null && !ptrReg.isBlank();
        if (!hasMac && !hasPtrReg) {
            return ResponseEntity.badRequest().build();
        }

        SseEmitter emitter;
        try {
            if (hasMac) {
                String compactMac = MacAddressNormalizer.toCompactForm(mac);
                emitter = broadcaster.subscribe(compactMac);
                notifier.notifyConnected(compactMac);
            } else {
                emitter = broadcaster.subscribeByPtrReg(ptrReg);
                broadcaster.sendTo(emitter, FiscalizacionSseEvent.connected(null));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache")
                .body(emitter);
    }
}
