package com.aeg.core.fiscalizacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "app.mqtt.inbound.enabled=false",
        "app.mqtt.enajenacion.enabled=false",
        "app.mqtt.fiscalizacion.enabled=true"
})
class FiscalizacionPayloadBuilderTest {

    @Autowired
    FiscalizacionPayloadBuilder payloadBuilder;

    @Autowired
    @Qualifier("mqttObjectMapper")
    ObjectMapper objectMapper;

    @Test
    void buildsSuccessAndErrorAcks() throws Exception {
        JsonNode ok = objectMapper.readTree(payloadBuilder.buildAckSuccess());
        assertThat(ok.path("cmd").asText()).isEqualTo("RxPtrFiscalizarRemoto");
        assertThat(ok.path("code").asInt()).isZero();
        assertThat(ok.path("data").path("msj").asText()).isEqualTo(FiscalizacionConstants.MSG_LISTA);

        JsonNode err = objectMapper.readTree(
                payloadBuilder.buildAckError(FiscalizacionConstants.MSG_MAC_EXISTE));
        assertThat(err.path("code").asInt()).isEqualTo(1);
        assertThat(err.path("data").path("msj").asText()).isEqualTo(FiscalizacionConstants.MSG_MAC_EXISTE);
    }

    @Test
    void resolveSealColorAcceptsSpanishLabel() {
        assertThat(FiscalizacionPreconditionValidator.resolveSealColor("Azul"))
                .isEqualTo(com.aeg.core.seal.SealColor.AZUL);
        assertThat(FiscalizacionPreconditionValidator.resolveSealColor("verde_neon"))
                .isEqualTo(com.aeg.core.seal.SealColor.VERDE_NEON);
    }
}
