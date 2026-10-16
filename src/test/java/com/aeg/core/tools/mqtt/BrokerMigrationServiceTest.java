package com.aeg.core.tools.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerConfigResult.Outcome;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.tools.mqtt.BrokerMigrationLegacyClient.LegacyBrokerTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Respuestas tomadas literalmente del documento "Protocolo para pasar impresora AEG del servidor antiguo al actual". */
class BrokerMigrationServiceTest {

    private static final String MAC = "F412FACECEB8";

    private static final String FIRM_OLD =
            "{\"cmd\":\"statFirmSinDNF\",\"code\":0,\"dataS\":\"FIRMWARE DEL SISTEMA: 1.1.0\",\"dataD\":2}";
    private static final String FIRM_NEW =
            "{\"cmd\":\"statFirmSinDNF\",\"code\":0,\"dataS\":\"FIRMWARE DEL SISTEMA: 1.1.1\",\"dataD\":2}";
    private static final String STAINF_OLD = """
            {"cmd":"StaInf","code":0,"dataS":{"host":"13.51.138.105","port":1883,
            "TopicCmd":"/F412FACECEB8/AEG_Fiscal/Integracion/Comando",
            "TopicRx":"/F412FACECEB8/AEG_Fiscal/Integracion/Respuesta"},"dataD":0}""";
    private static final String STAINF_NEW_DATAS = """
            {"host":"206.189.231.128","port":1883,"userName":"aegptrfiscal2024","password":"aegseniat2024",
            "TopicCmd":"/F412FACECEB8/AEG_Fiscal/Integracion/Comando",
            "TopicRx":"/F412FACECEB8/AEG_Fiscal/Integracion/Respuesta"}""";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BrokerMigrationLegacyClient legacyClient;
    private FiscalMqttSyncResponseAwaiter awaiter;
    private MqttService mqttService;
    private BrokerMigrationService service;

    @BeforeEach
    void setUp() {
        PrinterRepository repo = mock(PrinterRepository.class);
        Printer printer = new Printer();
        printer.setMacAddress("F4:12:FA:CE:CE:B8");
        when(repo.findById(1L)).thenReturn(Optional.of(printer));

        legacyClient = mock(BrokerMigrationLegacyClient.class);
        awaiter = mock(FiscalMqttSyncResponseAwaiter.class);
        mqttService = mock(MqttService.class);
        service = new BrokerMigrationService(
                repo, mock(SecurityScopeService.class), legacyClient, mqttService, awaiter, objectMapper) {
            @Override
            void pause(long millis) {
                // sin esperas en tests
            }
        };
    }

    private void legacyReplies(String cmd, String json) {
        when(legacyClient.publishAndAwait(eq(MAC), anyString(), eq(cmd), anyInt())).thenReturn(json);
    }

    private void legacyTimesOut(String cmd) {
        when(legacyClient.publishAndAwait(eq(MAC), anyString(), eq(cmd), anyInt()))
                .thenThrow(new LegacyBrokerTimeoutException("timeout"));
    }

    private void newBrokerReplies(FiscalMqttResponseItem item) {
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(CompletableFuture.completedFuture(item));
    }

    @Test
    void firmware110NeedsUpdate() {
        legacyReplies("statFirmSinDNF", FIRM_OLD);
        var result = service.checkFirmware(1L);
        assertEquals("1.1.0", result.firmwareVersion());
        assertTrue(result.needsUpdate());
    }

    @Test
    void firmware111DoesNotNeedUpdate() {
        legacyReplies("statFirmSinDNF", FIRM_NEW);
        var result = service.checkFirmware(1L);
        assertEquals("1.1.1", result.firmwareVersion());
        assertFalse(result.needsUpdate());
    }

    @Test
    void unknownFirmwareVersionFails() {
        legacyReplies("statFirmSinDNF",
                "{\"cmd\":\"statFirmSinDNF\",\"code\":0,\"dataS\":\"FIRMWARE DEL SISTEMA: 1.0.9\",\"dataD\":2}");
        assertThrows(EnajenacionProtocolException.class, () -> service.checkFirmware(1L));
    }

    @Test
    void unreadableFirmwareFails() {
        legacyReplies("statFirmSinDNF", "{\"cmd\":\"statFirmSinDNF\",\"code\":0,\"dataS\":\"\",\"dataD\":2}");
        assertThrows(EnajenacionProtocolException.class, () -> service.checkFirmware(1L));
    }

    @Test
    void firmwareFallsBackToNewBrokerForAlreadyMigratedPrinter() {
        legacyTimesOut("statFirmSinDNF");
        newBrokerReplies(new FiscalMqttResponseItem("statFirmSinDNF", 0, 2, "FIRMWARE DEL SISTEMA: 1.1.1"));
        var result = service.checkFirmware(1L);
        assertFalse(result.needsUpdate());
        verify(mqttService).publish(eq("/" + MAC + "/AEG_Fiscal/Integracion/Comando"), anyString());
    }

    @Test
    void oldBrokerDetectedOnLegacy() {
        legacyReplies("StaInf", STAINF_OLD);
        var result = service.checkBroker(1L);
        assertEquals("13.51.138.105", result.currentBrokerHost());
        assertTrue(result.isOldBroker());
    }

    @Test
    void newBrokerDetectedOnFallback() {
        legacyTimesOut("StaInf");
        // El awaiter serializa dataS-objeto como string JSON.
        newBrokerReplies(new FiscalMqttResponseItem("StaInf", 0, 0, STAINF_NEW_DATAS));
        var result = service.checkBroker(1L);
        assertEquals("206.189.231.128", result.currentBrokerHost());
        assertFalse(result.isOldBroker());
    }

    @Test
    void firmwareUpdateRejectedReturnsError() {
        legacyReplies("updFirmDown", "{\"cmd\":\"updFirmDown\",\"code\":3}");
        assertFalse(service.triggerFirmwareUpdate(1L).success());
    }

    @Test
    void firmwareUpdateWithoutAckIsAcceptedPendingVerification() {
        legacyTimesOut("updFirmDown");
        assertTrue(service.triggerFirmwareUpdate(1L).success());
    }

    @Test
    void configPayloadMatchesProtocol() throws Exception {
        JsonNode expected = objectMapper.readTree("""
                {"cmd":"configMqttAEG","data":{"addrBroker":"206.189.231.128","portBroker":1883,
                "userBroker":"aegptrfiscal2024","passwBroker":"aegseniat2024",
                "topicRx":"Comando","topicTx":"Respuesta","topicDoc":"Documento"}}""");
        assertEquals(expected, objectMapper.readTree(service.configMqttPayload()));
    }

    @Test
    void configureBrokerAckMigrates() {
        legacyReplies("configMqttAEG", "{\"cmd\":\"configMqttAEG\",\"code\":0}");
        assertEquals(Outcome.MIGRATED, service.configureBroker(1L).outcome());
        verify(mqttService, never()).publish(anyString(), anyString());
    }

    @Test
    void configureBrokerRejected() {
        legacyReplies("configMqttAEG", "{\"cmd\":\"configMqttAEG\",\"code\":1}");
        assertEquals(Outcome.REJECTED, service.configureBroker(1L).outcome());
    }

    @Test
    void configureBrokerLostAckConfirmedOnNewBroker() {
        legacyTimesOut("configMqttAEG");
        newBrokerReplies(new FiscalMqttResponseItem("StaInf", 0, 0, STAINF_NEW_DATAS));
        assertEquals(Outcome.MIGRATED, service.configureBroker(1L).outcome());
    }

    @Test
    void configureBrokerUncertainWhenNoBrokerAnswers() {
        legacyTimesOut("configMqttAEG");
        when(awaiter.registerWithMatcher(eq(MAC), any()))
                .thenAnswer(inv -> CompletableFuture.failedFuture(new RuntimeException("no reply")));
        assertEquals(Outcome.UNCERTAIN, service.configureBroker(1L).outcome());
    }

    @Test
    void versionHelpers() {
        assertEquals("1.1.0", BrokerMigrationService.extractVersion("FIRMWARE DEL SISTEMA: 1.1.0\n"));
        assertTrue(BrokerMigrationService.compareVersions("1.2", "1.1.1") > 0);
        assertEquals(0, BrokerMigrationService.compareVersions("1.1.1", "1.1.1"));
    }
}
