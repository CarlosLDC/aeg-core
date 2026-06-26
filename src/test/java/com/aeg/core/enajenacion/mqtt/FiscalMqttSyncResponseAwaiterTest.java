package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class FiscalMqttSyncResponseAwaiterTest {

    private static final String MAC = "206EF1884C68";
    private static final String RESPUESTA = "/" + MAC + "/AEG_Fiscal/Integracion/Respuesta";

    private FiscalMqttSyncResponseAwaiter awaiter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        awaiter = new FiscalMqttSyncResponseAwaiter(objectMapper);
    }

    @Test
    void completesPendingWaitOnMatchingStaInfResponse() throws Exception {
        CompletableFuture<FiscalMqttResponseItem> future = awaiter.register(MAC, EnajenacionConstants.CMD_STA_INF);
        String payload = "{\"cmd\":\"StaInf\",\"code\":0,\"dataS\":\"GRA0000017\",\"dataD\":0}";

        boolean handled = awaiter.tryComplete(MAC, RESPUESTA, payload);

        assertThat(handled).isTrue();
        assertThat(future.get()).isEqualTo(new FiscalMqttResponseItem("StaInf", 0, 0, "GRA0000017"));
    }

    @Test
    void ignoresNonMatchingCommand() {
        awaiter.register(MAC, EnajenacionConstants.CMD_STA_INF);
        String payload = "{\"cmd\":\"endFac\",\"code\":0,\"dataD\":1}";

        boolean handled = awaiter.tryComplete(MAC, RESPUESTA, payload);

        assertThat(handled).isFalse();
    }

    @Test
    void completesPendingWaitOnMatchingInvoiceArrayResponse() throws Exception {
        CompletableFuture<java.util.List<FiscalMqttResponseItem>> future = awaiter.registerArrayTerminal(
                MAC,
                EnajenacionConstants.CMD_END_FAC);
        String payload = """
                [
                  {"cmd":"proF","code":0,"dataD":0},
                  {"cmd":"subToF","code":0,"dataD":100},
                  {"cmd":"fpaF","code":0,"dataD":0},
                  {"cmd":"endFac","code":0,"dataD":7}
                ]
                """;

        boolean handled = awaiter.tryComplete(MAC, RESPUESTA, payload);

        assertThat(handled).isTrue();
        assertThat(future.get()).hasSize(4);
        assertThat(future.get().get(3)).isEqualTo(new FiscalMqttResponseItem("endFac", 0, 7));
    }

    @Test
    void completesPendingWaitOnMatchingCreditNoteArrayResponse() throws Exception {
        CompletableFuture<java.util.List<FiscalMqttResponseItem>> future = awaiter.registerArrayTerminal(
                MAC,
                EnajenacionConstants.CMD_END_NC);
        String payload = """
                [
                  {"cmd":"nroFacNC","code":0,"dataD":0},
                  {"cmd":"fechFacNC","code":0,"dataD":0},
                  {"cmd":"conSerNC","code":0,"dataD":0},
                  {"cmd":"rifCiNC","code":0,"dataD":0},
                  {"cmd":"razSocNC","code":0,"dataD":0},
                  {"cmd":"prodNC","code":0,"dataD":0},
                  {"cmd":"endPoNC","code":0,"dataD":0},
                  {"cmd":"fpaNC","code":0,"dataD":0},
                  {"cmd":"endNC","code":0,"dataD":10}
                ]
                """;

        boolean handled = awaiter.tryComplete(MAC, RESPUESTA, payload);

        assertThat(handled).isTrue();
        assertThat(future.get()).hasSize(9);
    }
}
