package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.aeg.core.mqtt.MqttService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class EnajenacionMqttOrchestratorTest {

    @Mock
    private EnajenacionPreconditionValidator preconditionValidator;

    @Mock
    private EnajenacionCompletionService completionService;

    @Mock
    private MqttService mqttService;

    private EnajenacionMqttOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EnajenacionPayloadBuilder payloadBuilder = new EnajenacionPayloadBuilder(objectMapper, "");
        EnajenacionSessionRegistry registry = new EnajenacionSessionRegistry();
        FiscalResponseValidator responseValidator = new FiscalResponseValidator();
        EnajenacionMqttSettings settings = new EnajenacionMqttSettings();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.initialize();

        orchestrator = new EnajenacionMqttOrchestrator(
                preconditionValidator,
                registry,
                payloadBuilder,
                responseValidator,
                completionService,
                mqttService,
                settings,
                objectMapper,
                scheduler);
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
