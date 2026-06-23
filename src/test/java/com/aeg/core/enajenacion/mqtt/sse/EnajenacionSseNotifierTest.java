package com.aeg.core.enajenacion.mqtt.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aeg.core.enajenacion.mqtt.EnajenacionContext;
import com.aeg.core.enajenacion.mqtt.EnajenacionSession;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class EnajenacionSseNotifierTest {

    private static final String MAC = "206EF1884C68";

    @Mock
    private EnajenacionSseBroadcaster broadcaster;

    private EnajenacionSseNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new EnajenacionSseNotifier(broadcaster);
    }

    @Test
    void notifyStepTransitionBroadcastsEvent() {
        EnajenacionSession session = sampleSession();
        notifier.notifyStepTransition(
                session,
                EnajenacionSessionState.DNF_SENT,
                "/206EF1884C68/AEG_Fiscal/Integracion/Respuesta",
                "[{\"cmd\":\"endDNF\",\"code\":0}]",
                "/206EF1884C68/AEG_Fiscal/Integracion/Comando",
                "{\"cmd\":\"fiscalAEG\"}");

        ArgumentCaptor<EnajenacionSseEvent> captor = ArgumentCaptor.forClass(EnajenacionSseEvent.class);
        verify(broadcaster).broadcast(eq(MAC), captor.capture());
        EnajenacionSseEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(EnajenacionSseEventType.STEP_TRANSITION);
        assertThat(event.acceptedStepId()).isEqualTo("dnf");
        assertThat(event.acceptedRespuestaPayload()).contains("endDNF");
        assertThat(event.publishedStepId()).isEqualTo("fiscal-rif");
    }

    @Test
    void broadcasterAcceptsSubscribeAndBroadcast() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EnajenacionSseBroadcaster liveBroadcaster = new EnajenacionSseBroadcaster(mapper);
        SseEmitter emitter = liveBroadcaster.subscribe(MAC);

        liveBroadcaster.broadcast(MAC, EnajenacionSseEvent.connected(MAC));

        emitter.complete();
    }

    @Test
    void notifySessionFailedUsesFailedAtState() {
        EnajenacionSession session = sampleSession();
        notifier.notifySessionFailed(
                session,
                "Timeout waiting for response at step FISCAL_RIF_SENT",
                EnajenacionSessionState.FISCAL_RIF_SENT);

        ArgumentCaptor<EnajenacionSseEvent> captor = ArgumentCaptor.forClass(EnajenacionSseEvent.class);
        verify(broadcaster).broadcast(eq(MAC), captor.capture());
        EnajenacionSseEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(EnajenacionSseEventType.SESSION_FAILED);
        assertThat(event.failedAtState()).isEqualTo(EnajenacionSessionState.FISCAL_RIF_SENT);
        assertThat(event.sessionState()).isEqualTo(EnajenacionSessionState.FAILED);
    }

    private static EnajenacionSession sampleSession() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                10L,
                "J500662998",
                "INVERSIONES SHOP COMPUTER 2020, C.A.",
                "CONTRIBUYENTE ORDINARIO",
                "LINE1",
                "LINE2",
                "CARACAS, DISTRITO CAPITAL");
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        session.setState(EnajenacionSessionState.FISCAL_RIF_SENT);
        return session;
    }
}
