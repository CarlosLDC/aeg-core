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
        assertThat(node.path("data").path("impFis").asInt()).isZero();
    }

    @Test
    void buildsReprintPayloadWithImpFisForPhysicalPrint() throws Exception {
        String payload = builder.reprintPayload("rFactura", 177, true);
        JsonNode node = new ObjectMapper().readTree(payload);

        assertThat(node.path("data").path("impFis").asInt()).isEqualTo(1);
    }

    @Test
    void buildsFooterWritePayloadAsLineArray() throws Exception {
        String payload = builder.footerWritePayload("LINEA 1\nLINEA 2\n");
        JsonNode node = new ObjectMapper().readTree(payload);

        assertThat(node.path("cmd").asText()).isEqualTo("pieTiF");
        assertThat(node.path("data").isArray()).isTrue();
        assertThat(node.path("data").get(0).asText()).isEqualTo("LINEA 1");
        assertThat(node.path("data").get(1).asText()).isEqualTo("LINEA 2");
    }

    @Test
    void buildsHeaderWritePayloadWithEncFacFijoArray() throws Exception {
        String payload = builder.headerWritePayload("ENC 1\nENC 2");
        JsonNode node = new ObjectMapper().readTree(payload);

        assertThat(node.path("cmd").asText()).isEqualTo("wFileSPIFF");
        assertThat(node.path("data").path("Access").asText()).isEqualTo("AeG-1968-2024");
        assertThat(node.path("data").path("nameFile").asText()).isEqualTo("paramFacSPIFF.json");
        assertThat(node.path("data").path("contenido").path("encFacFijo").get(0).asText())
                .isEqualTo("ENC 1");
        assertThat(node.path("data").path("contenido").path("encFacFijo").get(1).asText())
                .isEqualTo("ENC 2");
    }
}
