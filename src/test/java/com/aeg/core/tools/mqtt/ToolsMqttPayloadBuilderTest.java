package com.aeg.core.tools.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ToolsMqttPayloadBuilderTest {

    private final ToolsMqttPayloadBuilder builder =
            new ToolsMqttPayloadBuilder(new ObjectMapper());

    @Test
    void buildsReprintPayloadWithTipoReAndNroRegArray() throws Exception {
        String payload = builder.reprintPayload("rFactura", 177);
        JsonNode node = new ObjectMapper().readTree(payload);

        assertThat(node.path("cmd").asText()).isEqualTo("reimRep");
        assertThat(node.path("data").path("tipoRe").asText()).isEqualTo("rFactura");
        assertThat(node.path("data").path("nroReg").get(0).asInt()).isEqualTo(177);
    }

    @Test
    void buildsReportZReprintPayload() throws Exception {
        String payload = builder.reprintPayload("rReporZ", 6);
        JsonNode node = new ObjectMapper().readTree(payload);

        assertThat(node.path("data").path("tipoRe").asText()).isEqualTo("rReporZ");
        assertThat(node.path("data").path("nroReg").get(0).asInt()).isEqualTo(6);
    }
}
