package com.aeg.core.tools.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.ToolsMqttStatusResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.tools.mqtt.BrokerMigrationLegacyClient.LegacyBrokerTimeoutException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ToolsMqttServiceTest {

    private static final String MAC = "F412FACECEB8";
    private static final String STATUS_DATA_S = """
            {"EstatusSeniat":"EN LINEA","ConexionWifi":"AP_Oficina","direccionIP":"192.168.1.50","NroUltZEmit":12,"NroUltZTx":11,"DiasSinTx":0}""";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BrokerMigrationLegacyClient legacyClient;
    private FiscalMqttSyncResponseAwaiter awaiter;
    private MqttService mqttService;
    private ToolsMqttService service;

    @BeforeEach
    void setUp() {
        PrinterRepository repo = mock(PrinterRepository.class);
        Printer printer = new Printer();
        printer.setMacAddress("F4:12:FA:CE:CE:B8");
        when(repo.findById(1L)).thenReturn(Optional.of(printer));

        legacyClient = mock(BrokerMigrationLegacyClient.class);
        awaiter = mock(FiscalMqttSyncResponseAwaiter.class);
        mqttService = mock(MqttService.class);
        ToolsMqttPayloadBuilder payloadBuilder = new ToolsMqttPayloadBuilder(objectMapper);
        ToolsMqttResponseParser responseParser = new ToolsMqttResponseParser(objectMapper);
        ToolsMqttSettings settings = mock(ToolsMqttSettings.class);
        when(settings.statusTimeoutSeconds()).thenReturn(2);

        service = new ToolsMqttService(
                repo,
                mock(SecurityScopeService.class),
                mqttService,
                awaiter,
                payloadBuilder,
                responseParser,
                settings,
                legacyClient);
    }

    @Test
    void requestStatusReturnsNewBrokerWhenAnsweredOnMainBroker() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 0, STATUS_DATA_S);
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(CompletableFuture.completedFuture(item));
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(), anyInt()))
                .thenThrow(new LegacyBrokerTimeoutException("timeout"));

        ToolsMqttStatusResponse status = service.requestStatus(1L);

        assertTrue(status.success());
        assertEquals("EN LINEA", status.seniatStatus());
        assertEquals("AP_Oficina", status.additionalInfo().wifiNetwork());
        assertEquals("192.168.1.50", status.additionalInfo().ipAddress());
        assertFalse(Boolean.TRUE.equals(status.legacyBroker()));
    }

    @Test
    void requestStatusDetectsWifiStatusFromLegacyBroker() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 0, STATUS_DATA_S);
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(new CompletableFuture<>());
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(), anyInt())).thenReturn(item);

        ToolsMqttStatusResponse status = service.requestStatus(1L);

        assertTrue(status.success());
        assertEquals("EN LINEA", status.seniatStatus());
        assertEquals("AP_Oficina", status.additionalInfo().wifiNetwork());
        assertEquals("192.168.1.50", status.additionalInfo().ipAddress());
        assertTrue(Boolean.TRUE.equals(status.legacyBroker()));
    }

    @Test
    void requestStatusThrowsTimeoutWhenNeitherBrokerAnswers() {
        CompletableFuture<FiscalMqttResponseItem> failedMain = new CompletableFuture<>();
        failedMain.completeExceptionally(new LegacyBrokerTimeoutException("timeout"));
        when(awaiter.registerWithMatcher(eq(MAC), any())).thenReturn(failedMain);
        when(legacyClient.publishAndAwaitItem(eq(MAC), anyString(), any(), anyInt()))
                .thenThrow(new LegacyBrokerTimeoutException("timeout"));

        EnajenacionProtocolException ex = assertThrows(
                EnajenacionProtocolException.class, () -> service.requestStatus(1L));
        assertTrue(ex.getMessage().contains("La impresora no respondió a tiempo"));
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
}
