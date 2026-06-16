package com.aeg.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MqttConfigTest {

    @Test
    void inboundTopicsIncludeFiscalCmdServerWhenEnajenacionIsEnabled() {
        MqttConfig config = new MqttConfig(null);
        ReflectionTestUtils.setField(config, "inboundTopic", "aeg/telemetry/#");
        ReflectionTestUtils.setField(config, "enajenacionEnabled", true);
        ReflectionTestUtils.setField(config, "enajenacionInboundTopic", "+/AEG_Fiscal/Integracion/CmdServer");

        assertThat(config.inboundTopics())
                .containsExactly(
                        "aeg/telemetry/#",
                        "+/AEG_Fiscal/Integracion/CmdServer",
                        "/+/AEG_Fiscal/Integracion/CmdServer");
    }

    @Test
    void inboundTopicsDeduplicateWhenMonitorAlreadyUsesFiscalCmdServer() {
        MqttConfig config = new MqttConfig(null);
        ReflectionTestUtils.setField(config, "inboundTopic", "+/AEG_Fiscal/Integracion/CmdServer");
        ReflectionTestUtils.setField(config, "enajenacionEnabled", true);
        ReflectionTestUtils.setField(config, "enajenacionInboundTopic", "+/AEG_Fiscal/Integracion/CmdServer");

        assertThat(config.inboundTopics())
                .containsExactly(
                        "+/AEG_Fiscal/Integracion/CmdServer",
                        "/+/AEG_Fiscal/Integracion/CmdServer");
    }
}
