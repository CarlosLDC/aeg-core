package com.aeg.core.fiscalizacion.mqtt.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aeg.core.fiscalizacion.mqtt.FiscalizacionSessionState;
import com.aeg.core.security.AdminJwtTokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class FiscalizacionSseBroadcasterTest {

    @Test
    void subscribeByPtrRegAndBroadcastReachesBothMacAndRegSubscribers() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        FiscalizacionSseBroadcaster broadcaster = new FiscalizacionSseBroadcaster(mapper);

        SseEmitter macEmitter = broadcaster.subscribe("206EF1884C68");
        SseEmitter regEmitter = broadcaster.subscribeByPtrReg("gra0000017");

        FiscalizacionSseEvent event = FiscalizacionSseEvent.sessionStarted(
                "206EF1884C68",
                "GRA0000017",
                "ack",
                "/206EF1884C68/AEG_Fiscal/Integracion/Comando",
                "{\"cmd\":\"RxPtrFiscalizarRemoto\",\"code\":0}",
                FiscalizacionSessionState.ACK_SENT);

        broadcaster.broadcast("206EF1884C68", event);

        macEmitter.complete();
        regEmitter.complete();
    }

    @Test
    void controllerAcceptsPtrRegWithoutMac() throws Exception {
        AdminJwtTokenValidator validator = mock(AdminJwtTokenValidator.class);
        when(validator.validateAdminToken("tok")).thenReturn(Optional.of("admin@aeg.com"));
        FiscalizacionSseBroadcaster broadcaster = mock(FiscalizacionSseBroadcaster.class);
        SseEmitter emitter = new SseEmitter();
        when(broadcaster.subscribeByPtrReg("GRA0000017")).thenReturn(emitter);
        FiscalizacionSseNotifier notifier = mock(FiscalizacionSseNotifier.class);

        FiscalizacionSseController controller = new FiscalizacionSseController(validator, broadcaster, notifier);

        ResponseEntity<?> response = controller.stream(null, "GRA0000017", "tok", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(broadcaster).subscribeByPtrReg("GRA0000017");
        verify(broadcaster).sendTo(any(), any());
    }

    @Test
    void controllerRejectsWhenNeitherMacNorPtrRegIsProvided() {
        AdminJwtTokenValidator validator = mock(AdminJwtTokenValidator.class);
        when(validator.validateAdminToken("tok")).thenReturn(Optional.of("admin@aeg.com"));
        FiscalizacionSseController controller = new FiscalizacionSseController(
                validator, mock(FiscalizacionSseBroadcaster.class), mock(FiscalizacionSseNotifier.class));

        ResponseEntity<?> response = controller.stream("  ", "", "tok", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
