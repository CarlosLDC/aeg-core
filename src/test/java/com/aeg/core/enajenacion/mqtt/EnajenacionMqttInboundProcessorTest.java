package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityRecorder;

@ExtendWith(MockitoExtension.class)
class EnajenacionMqttInboundProcessorTest {

    private static final String TOPIC = "/206EF1884C68/AEG_Fiscal/Integracion/CmdServer";
    private static final String PAYLOAD =
            "{\"cmd\":\"ptrEnajenar\",\"data\":{\"ptrReg\":\"GRA0000017\",\"macAddr\":\"20:6E:F1:88:4C:68\"}}";

    @Mock
    private EnajenacionMqttOrchestrator orchestrator;

    @Mock
    private EnajenacionMqttSettings settings;

    @Mock
    private EnajenacionSessionRegistry sessionRegistry;

    @Mock
    private EnajenacionActivityRecorder activityRecorder;

    private EnajenacionMqttInboundProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EnajenacionMqttInboundProcessor(
                orchestrator, settings, sessionRegistry, activityRecorder);
        when(settings.enabled()).thenReturn(true);
    }

    @Test
    void processesFiscalInboundOncePerPayload() {
        when(orchestrator.handleInboundWithOutcome(eq(TOPIC), eq(PAYLOAD)))
                .thenReturn(Optional.of(EnajenacionStartOutcome.started()));

        Optional<EnajenacionStartOutcome> first = processor.process(TOPIC, PAYLOAD);
        Optional<EnajenacionStartOutcome> second = processor.process(TOPIC, PAYLOAD);

        assertThat(first).contains(EnajenacionStartOutcome.started());
        assertThat(second).isEmpty();
        verify(orchestrator, times(1)).handleInboundWithOutcome(TOPIC, PAYLOAD);
    }

    @Test
    void skipsWhenEnajenacionDisabled() {
        when(settings.enabled()).thenReturn(false);

        assertThat(processor.process(TOPIC, PAYLOAD)).isEmpty();
        verify(orchestrator, org.mockito.Mockito.never())
                .handleInboundWithOutcome(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
    }
}
