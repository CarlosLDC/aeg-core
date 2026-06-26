package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityRecorder;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityStore;
import com.aeg.core.enajenacion.mqtt.activity.InMemoryEnajenacionActivityPersistence;
import com.aeg.core.enajenacion.mqtt.sse.EnajenacionSseNotifier;
import com.aeg.core.mqtt.MqttService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class EnajenacionMqttOrchestratorTest {

    private static final String MAC = "206EF1884C68";
    private static final String CMD_SERVER = "/" + MAC + "/AEG_Fiscal/Integracion/CmdServer";
    private static final String RESPUESTA = "/" + MAC + "/AEG_Fiscal/Integracion/Respuesta";

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
    private EnajenacionActivityStore activityStore;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EnajenacionPayloadBuilder payloadBuilder = new EnajenacionPayloadBuilder(objectMapper, "");
        registry = new EnajenacionSessionRegistry();
        activityStore = new EnajenacionActivityStore(new InMemoryEnajenacionActivityPersistence());
        EnajenacionActivityRecorder activityRecorder = new EnajenacionActivityRecorder(activityStore);
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
                sseNotifier,
                activityRecorder);
    }

    @Test
    void bareNumericFirmwarePayloadIsIgnoredWhileAwaitingFiscalRif() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.FISCAL_RIF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.OBJECT);

        orchestrator.handleInbound(RESPUESTA, "-1");

        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.FISCAL_RIF_SENT);
        verify(mqttService, org.mockito.Mockito.never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        assertThat(activityStore.recent(20, MAC))
                .anyMatch(entry -> entry.result() == EnajenacionActivityResult.IGNORED
                        && entry.detail() != null
                        && entry.detail().contains("Unrecognized JSON object response"));
    }

    @Test
    void validFiscalRifResponseAfterIgnoredNumericPayload() {
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
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.FISCAL_RIF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.OBJECT);

        orchestrator.handleInbound(RESPUESTA, "-1");
        orchestrator.handleInbound(RESPUESTA, EnajenacionMqttResponses.fiscalRifSuccess());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, times(1)).publish(eq("/" + MAC + "/AEG_Fiscal/Integracion/Comando"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"cmd\":\"wFileSPIFF\"");
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.HEADER_SENT);
    }

    @Test
    void fiscalRifResponseOnRespuestaPublishesHeader() {
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
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.FISCAL_RIF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.OBJECT);

        orchestrator.handleInbound(RESPUESTA, EnajenacionMqttResponses.fiscalRifSuccess());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, times(1)).publish(eq("/" + MAC + "/AEG_Fiscal/Integracion/Comando"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"cmd\":\"wFileSPIFF\"");
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.HEADER_SENT);
    }

    @Test
    void fiscalRifResponseWaitsForSessionLockDuringDnfTransition() throws Exception {
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenReturn(mock(ScheduledFuture.class));

        CountDownLatch holdDuringFiscalPublish = new CountDownLatch(1);
        CountDownLatch releasePublish = new CountDownLatch(1);
        AtomicInteger publishCount = new AtomicInteger();
        doAnswer(invocation -> {
            if (publishCount.incrementAndGet() == 1) {
                holdDuringFiscalPublish.countDown();
                releasePublish.await(5, TimeUnit.SECONDS);
            }
            return null;
        }).when(mqttService).publish(any(String.class), any(String.class));

        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.DNF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        Thread dnfThread = new Thread(() -> orchestrator.handleInbound(RESPUESTA, EnajenacionMqttResponses.dnfSuccess()));
        dnfThread.start();
        assertThat(holdDuringFiscalPublish.await(5, TimeUnit.SECONDS)).isTrue();

        Thread fiscalResponseThread = new Thread(
                () -> orchestrator.handleInbound(RESPUESTA, EnajenacionMqttResponses.fiscalRifSuccess()));
        fiscalResponseThread.start();
        releasePublish.countDown();
        dnfThread.join(5_000);
        fiscalResponseThread.join(5_000);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, times(2)).publish(eq("/" + MAC + "/AEG_Fiscal/Integracion/Comando"), payloadCaptor.capture());
        assertThat(payloadCaptor.getAllValues().get(0)).contains("\"cmd\":\"fiscalAEG\"");
        assertThat(payloadCaptor.getAllValues().get(1)).contains("\"cmd\":\"wFileSPIFF\"");
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.HEADER_SENT);
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
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.DNF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        String dnfResponse = EnajenacionMqttResponses.dnfSuccess();
        orchestrator.handleInbound(RESPUESTA, dnfResponse);
        orchestrator.handleInbound(RESPUESTA, dnfResponse);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, times(1)).publish(eq("/" + MAC + "/AEG_Fiscal/Integracion/Comando"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"cmd\":\"fiscalAEG\"");
        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.FISCAL_RIF_SENT);
    }

    @Test
    void dnfResponseRecordsProcessedInboundAndPublishedOutbound() {
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
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.DNF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        orchestrator.handleInbound(RESPUESTA, EnajenacionMqttResponses.dnfSuccess());

        assertThat(activityStore.recent(20, MAC))
                .anyMatch(entry -> entry.result() == EnajenacionActivityResult.PROCESSED
                        && entry.direction() != null
                        && entry.direction().name().equals("INBOUND"));
        assertThat(activityStore.recent(20, MAC))
                .anyMatch(entry -> entry.result() == EnajenacionActivityResult.PUBLISHED
                        && entry.direction() != null
                        && entry.direction().name().equals("OUTBOUND"));
    }

    @Test
    void staleDnfResponseWhileAwaitingInvoiceDoesNotFailSession() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.INVOICE_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        orchestrator.handleInbound(RESPUESTA, EnajenacionMqttResponses.dnfSuccess());

        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.INVOICE_SENT);
        verify(mqttService, org.mockito.Mockito.never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void dnfComandoPayloadOnCmdServerIsIgnoredWhileAwaitingDnfResponse() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.DNF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        String dnfCommand =
                """
                [{"cmd":"aperDNF","dataS":"DOCUMENTO NO FISCAL"},{"cmd":"endDNF","dataS":"WAIT"}]
                """;
        orchestrator.handleInbound(CMD_SERVER, dnfCommand);

        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.DNF_SENT);
        verify(mqttService, org.mockito.Mockito.never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
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
        assertThat(FiscalMqttTopics.extractCompactMac("/206EF1884C68/AEG_Fiscal/Integracion/Respuesta"))
                .contains("206EF1884C68");
        assertThat(FiscalMqttTopics.respuestaTopic(MAC)).isEqualTo(RESPUESTA);
    }

    @Test
    void dnfResponseOnCmdServerIsIgnoredWhileAwaitingDnf() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.DNF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.ARRAY);

        orchestrator.handleInbound(CMD_SERVER, EnajenacionMqttResponses.dnfSuccess());

        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.DNF_SENT);
        verify(mqttService, org.mockito.Mockito.never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void mismatchedFiscalRifResponseIsIgnoredWhileAwaitingFiscalRif() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                java.util.List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        registry.register(session);
        session.setState(EnajenacionSessionState.FISCAL_RIF_SENT);
        session.setAwaiting(EnajenacionAwaitingKind.OBJECT);

        orchestrator.handleInbound(RESPUESTA, "{\"cmd\":\"wFileSPIFF\",\"code\":0,\"dataD\":0}");

        assertThat(registry.find(MAC)).isPresent();
        assertThat(registry.find(MAC).orElseThrow().state()).isEqualTo(EnajenacionSessionState.FISCAL_RIF_SENT);
        verify(mqttService, org.mockito.Mockito.never()).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
