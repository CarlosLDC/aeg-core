package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import com.aeg.core.enajenacion.mqtt.sse.EnajenacionSseNotifier;
import com.aeg.core.mqtt.MqttService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class EnajenacionMqttOrchestratorTest {

    private static final String MAC = "206EF1884C68";
    private static final String CMD_SERVER = "/" + MAC + "/AEG_Fiscal/Integracion/CmdServer";

    @Mock
    private EnajenacionPreconditionValidator preconditionValidator;

    @Mock
    private EnajenacionCompletionService completionService;

    @Mock
    private MqttService mqttService;

    @Mock
    private EnajenacionSseNotifier sseNotifier;

    @Mock
    private TaskScheduler taskScheduler;

    private EnajenacionMqttOrchestrator orchestrator;
    private EnajenacionSessionRegistry registry;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EnajenacionPayloadBuilder payloadBuilder = new EnajenacionPayloadBuilder(objectMapper, "");
        registry = new EnajenacionSessionRegistry();
        FiscalResponseValidator responseValidator = new FiscalResponseValidator();
        EnajenacionMqttSettings settings = new EnajenacionMqttSettings();
        orchestrator = new EnajenacionMqttOrchestrator(
                preconditionValidator,
                registry,
                payloadBuilder,
                responseValidator,
                completionService,
                mqttService,
                settings,
                objectMapper,
                taskScheduler,
                sseNotifier);
    }

    @Test
    void dnfResponsePublishesFiscalRifAndIgnoresDuplicateEcho() {
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenReturn(mock(ScheduledFuture.class));

        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC");
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.DNF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        String dnfResponse = EnajenacionMqttResponses.dnfSuccess();
        orchestrator.handleInbound(CMD_SERVER, dnfResponse);
        orchestrator.handleInbound(CMD_SERVER, dnfResponse);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, times(1)).publish(eq("/" + MAC + "/AEG_Fiscal/Integracion/Comando"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"cmd\":\"fiscalAEG\"");
        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.FISCAL_RIF_SENT);
    }

    @Test
    void ignoresNonFiscalTopics() {
        orchestrator.handleInbound("aeg/telemetry/device-1", "{\"cmd\":\"ptrEnajenar\"}");
        verify(mqttService, org.mockito.Mockito.never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void extractsMacFromFiscalTopic() {
        assertThat(FiscalMqttTopics.extractCompactMac("/206EF1884C68/AEG_Fiscal/Integracion/CmdServer"))
                .contains("206EF1884C68");
        assertThat(FiscalMqttTopics.extractCompactMac("206EF1884C68/AEG_Fiscal/Integracion/CmdServer"))
                .contains("206EF1884C68");
    }
}
