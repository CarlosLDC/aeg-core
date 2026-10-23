package com.aeg.core.tools.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter.ToolsMqttTextChunksResult;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.ToolsFormasPagoReadResponse;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.mqtt.dto.ToolsMqttStatusResponse;
import com.aeg.core.mqtt.dto.ToolsReportZResponse;
import com.aeg.core.mqtt.dto.ToolsReprintResponse;
import com.aeg.core.mqtt.dto.ToolsWifiScanResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.tools.mqtt.BrokerMigrationLegacyClient.LegacyBrokerTimeoutException;
import com.aeg.core.tools.mqtt.BrokerMigrationLegacyClient.LegacyTextChunksState;
import com.fasterxml.jackson.databind.ObjectMapper;

class ToolsMqttServiceTest {

    private static final String MAC = "F412FACECEB8";
    private static final String STATUS_DATA_S = """
            {"EstatusSeniat":"EN LINEA","ConexionWifi":"AP_Oficina","direccionIP":"192.168.1.50","NroUltZEmit":12,"NroUltZTx":11,"DiasSinTx":0}""";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PrinterRepository repo;
    private SecurityScopeService securityScope;
    private BrokerMigrationLegacyClient legacyClient;
    private FiscalMqttSyncResponseAwaiter awaiter;
    private MqttService mqttService;
    private ToolsMqttSettings settings;
    private ToolsMqttService service;
    private ToolsTestDocumentsService testDocumentsService;

    @BeforeEach
    void setUp() {
        repo = mock(PrinterRepository.class);
        Printer printer = new Printer();
        printer.setMacAddress("F4:12:FA:CE:CE:B8");
        printer.setFiscalSerial("GRA0000017");
        when(repo.findById(1L)).thenReturn(Optional.of(printer));

        securityScope = mock(SecurityScopeService.class);
        legacyClient = mock(BrokerMigrationLegacyClient.class);
        awaiter = mock(FiscalMqttSyncResponseAwaiter.class);
        mqttService = mock(MqttService.class);
        ToolsMqttPayloadBuilder payloadBuilder = new ToolsMqttPayloadBuilder(objectMapper);
        ToolsMqttResponseParser responseParser = new ToolsMqttResponseParser(objectMapper);
        settings = mock(ToolsMqttSettings.class);
        when(settings.statusTimeoutSeconds()).thenReturn(2);
        when(settings.wifiScanTimeoutSeconds()).thenReturn(2);
        when(settings.wifiTimeoutSeconds()).thenReturn(2);
        when(settings.reportZTimeoutSeconds()).thenReturn(2);
        when(settings.formasPagoTimeoutSeconds()).thenReturn(2);
        when(settings.reprintTimeoutSeconds()).thenReturn(2);
        when(settings.defaultTimeoutSeconds()).thenReturn(2);
        when(settings.testInvoiceTimeoutSeconds()).thenReturn(2);
        when(settings.testNoteTimeoutSeconds()).thenReturn(2);
        when(settings.testGenerateZTimeoutSeconds()).thenReturn(2);

        service = new ToolsMqttService(
                repo,
                securityScope,
                mqttService,
                awaiter,
                payloadBuilder,
                responseParser,
                settings,
                legacyClient);

        testDocumentsService = new ToolsTestDocumentsService(
                repo,
                securityScope,
                mqttService,
                awaiter,
                new ToolsTestDocumentsPayloadBuilder(objectMapper),
                settings,
                legacyClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestStatusReturnsNewBrokerWhenAnsweredOnMainBroker() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 0, STATUS_DATA_S);
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(CompletableFuture.completedFuture(item));
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(Predicate.class), anyInt()))
                .thenThrow(new LegacyBrokerTimeoutException("timeout"));

        ToolsMqttStatusResponse status = service.requestStatus(1L);

        assertTrue(status.success());
        assertEquals("EN LINEA", status.seniatStatus());
        assertEquals("AP_Oficina", status.additionalInfo().wifiNetwork());
        assertEquals("192.168.1.50", status.additionalInfo().ipAddress());
        assertFalse(Boolean.TRUE.equals(status.legacyBroker()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestStatusDetectsWifiStatusFromLegacyBroker() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 0, STATUS_DATA_S);
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(new CompletableFuture<>());
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(Predicate.class), anyInt())).thenReturn(item);

        ToolsMqttStatusResponse status = service.requestStatus(1L);

        assertTrue(status.success());
        assertEquals("EN LINEA", status.seniatStatus());
        assertEquals("AP_Oficina", status.additionalInfo().wifiNetwork());
        assertEquals("192.168.1.50", status.additionalInfo().ipAddress());
        assertTrue(Boolean.TRUE.equals(status.legacyBroker()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestStatusThrowsTimeoutWhenNeitherBrokerAnswers() {
        CompletableFuture<FiscalMqttResponseItem> failedMain = new CompletableFuture<>();
        failedMain.completeExceptionally(new LegacyBrokerTimeoutException("timeout"));
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(failedMain);
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(Predicate.class), anyInt()))
                .thenThrow(new LegacyBrokerTimeoutException("timeout"));

        EnajenacionProtocolException ex = assertThrows(
                EnajenacionProtocolException.class, () -> service.requestStatus(1L));
        assertTrue(ex.getMessage().contains("La impresora no respondió a tiempo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void wifiAndFormasPagoAndReportZFallbackToLegacyBroker() {
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(new CompletableFuture<>());
        when(awaiter.register(eq(MAC), anyString())).thenReturn(new CompletableFuture<>());

        FiscalMqttResponseItem wifiItem = new FiscalMqttResponseItem(
                "StaInf", 0, 0, "[{\"ssid\":\"RedFiscal\",\"rssi\":-50}]");
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(Predicate.class), anyInt()))
                .thenReturn(wifiItem);

        ToolsWifiScanResponse scanResponse = service.scanWifi(1L);
        assertTrue(scanResponse.success());
        assertEquals(1, scanResponse.networks().size());
        assertEquals("RedFiscal", scanResponse.networks().get(0).ssid());

        FiscalMqttResponseItem fpItem = new FiscalMqttResponseItem(
                "StaInf", 0, 0, "{\"1\":\"[1]EFECTIVO\",\"2\":\"[2]PUNTO DE VENTA\"}");
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(Predicate.class), anyInt()))
                .thenReturn(fpItem);

        ToolsFormasPagoReadResponse fpResponse = service.readFormasPago(1L);
        assertTrue(fpResponse.success());
        assertEquals(2, fpResponse.formasPago().size());

        FiscalMqttResponseItem repZItem = new FiscalMqttResponseItem(
                "getRepZ", 0, 0, "{\"NroZ\":15,\"FechaZ\":\"23/10/2026\"}");
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), eq(ToolsMqttConstants.CMD_GET_REP_Z), anyInt()))
                .thenReturn(repZItem);

        ToolsReportZResponse zResponse = service.listReportZ(1L);
        assertTrue(zResponse.success());
        assertEquals(15, zResponse.report().report().get("NroZ"));
    }

    @Test
    void reprintAndResetWifiAndTestDocumentsWorkOnLegacyBrokerContingency() {
        when(awaiter.registerTextChunks(eq(MAC), eq(ToolsMqttConstants.CMD_REIM_REP)))
                .thenReturn(new CompletableFuture<>());
        ToolsMqttTextChunksResult chunks = new ToolsMqttTextChunksResult(
                List.of("SENIAT\n", "FACTURA #1\n"),
                new FiscalMqttResponseItem(ToolsMqttConstants.CMD_REIM_REP, 0, 0, null));
        when(legacyClient.publishAndAwaitTextChunks(eq(MAC), anyString(), eq(ToolsMqttConstants.CMD_REIM_REP), anyInt()))
                .thenReturn(chunks);

        ToolsReprintResponse reprintResponse = service.reprint(1L, "FAC", 1, "visualize");
        assertTrue(reprintResponse.success());
        assertTrue(reprintResponse.escPosContent().contains("FACTURA #1"));

        doThrow(new RuntimeException("main broker down"))
                .when(mqttService).publish(anyString(), anyString());

        ToolsMqttSimpleResponse resetResponse = service.resetWifi(1L);
        assertTrue(resetResponse.success());
        verify(legacyClient).publishFireAndForget(eq(MAC), anyString());

        when(awaiter.register(eq(MAC), eq(ToolsMqttConstants.CMD_END_FAC)))
                .thenReturn(new CompletableFuture<>());
        when(legacyClient.publishSequenceAndAwaitItem(
                eq(MAC), anyList(), anyInt(), eq(ToolsMqttConstants.CMD_END_FAC), anyInt()))
                .thenReturn(new FiscalMqttResponseItem(ToolsMqttConstants.CMD_END_FAC, 0, 0, null));

        ToolsMqttSimpleResponse testInvoice = testDocumentsService.sendTestInvoice(1L);
        assertTrue(testInvoice.success());
    }

    @Test
    void legacyClientExtractsMatchingResponseItemFromObjectAndArray() {
        BrokerMigrationLegacyClient realLegacyClient = new BrokerMigrationLegacyClient(objectMapper);
        String rawObject = """
                {"cmd":"StaInf","code":0,"dataD":0,"dataS":{"EstatusSeniat":"EN LINEA","ConexionWifi":"MiRed","direccionIP":"10.0.0.15"}}""";
        FiscalMqttResponseItem extracted = realLegacyClient.extractMatchingResponseItem(
                rawObject, ToolsMqttResponseParser::isStatusResponse);
        assertNotNull(extracted);
        assertTrue(extracted.dataS().contains("MiRed"));

        String rawArray = "[" + rawObject + "]";
        FiscalMqttResponseItem extractedFromArray = realLegacyClient.extractMatchingResponseItem(
                rawArray, ToolsMqttResponseParser::isStatusResponse);
        assertNotNull(extractedFromArray);
        assertTrue(extractedFromArray.dataS().contains("10.0.0.15"));
    }

    @Test
    void legacyClientProcessesTextChunksFromDocumentoAndRespuestaTopics() {
        BrokerMigrationLegacyClient realLegacyClient = new BrokerMigrationLegacyClient(objectMapper);
        String docTopic = "/" + MAC + "/AEG_Fiscal/Integracion/Documento";
        String respTopic = "/" + MAC + "/AEG_Fiscal/Integracion/Respuesta";

        LegacyTextChunksState docState = new LegacyTextChunksState("reimRep");
        assertNull(realLegacyClient.processTextChunksMessage(docState, docTopic, "LINEA 1"));
        assertNull(realLegacyClient.processTextChunksMessage(docState, docTopic, "LINEA 2"));
        ToolsMqttTextChunksResult docResult = realLegacyClient.processTextChunksMessage(docState, docTopic, "-1");
        assertNotNull(docResult);
        assertEquals(2, docResult.chunks().size());
        assertEquals(0, docResult.terminal().code());

        LegacyTextChunksState respState = new LegacyTextChunksState("impRepX");
        assertNull(realLegacyClient.processTextChunksMessage(respState, respTopic, "REPORTE X\n"));
        ToolsMqttTextChunksResult respResult = realLegacyClient.processTextChunksMessage(
                respState, respTopic, "{\"cmd\":\"impRepX\",\"code\":0}");
        assertNotNull(respResult);
        assertEquals(1, respResult.chunks().size());
        assertEquals(0, respResult.terminal().code());
    }
}
