package com.aeg.core.tools.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.dto.ToolsMqttStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class ToolsMqttResponseParserTest {

    private ToolsMqttResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ToolsMqttResponseParser(new ObjectMapper());
    }

    @Test
    void parseStatusReturnsSeniatOnline() {
        String dataS = """
                {"EstatusSeniat":"EN LINEA","ConexionWifi":"AEG-WiFi","direccionIP":"192.168.1.10",\
                "NroUltZEmit":42,"NroUltZTx":41,"DiasSinTx":0}""";
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, null, dataS);

        ToolsMqttStatusResponse response = parser.parseStatus(item);

        assertTrue(response.success());
        assertEquals("EN LINEA", response.seniatStatus());
        assertEquals("AEG-WiFi", response.additionalInfo().wifiNetwork());
        assertEquals("192.168.1.10", response.additionalInfo().ipAddress());
        assertEquals(42, response.additionalInfo().lastZReport());
    }

    @Test
    void parseStatusReturnsSinConexionForPrinterMessage() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem(
                "StaInf", 0, null, "Impresora Apagada");

        ToolsMqttStatusResponse response = parser.parseStatus(item);

        assertTrue(response.success());
        assertEquals("SIN CONEXION", response.seniatStatus());
    }

    @Test
    void isWifiScanResponseDetectsJsonArray() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem(
                "StaInf", 0, null, "[{\"ssid\":\"Red1\",\"rssi\":-50}]");

        assertTrue(ToolsMqttResponseParser.isWifiScanResponse(item));
        assertFalse(ToolsMqttResponseParser.isStatusResponse(item));
    }

    @Test
    void isHeaderFooterReadResponseDetectsStringArray() {
        String dataS = """
                ["CALLE PRINCIPAL CC ORINOKIA MALL NIVEL PB",\
                "LOCAL PB-C-064A/PB-C-064B SECTOR ALTA VISTA",\
                "PUERTO ORDAZ ZONA POSTAL 8050",\
                "CIUDAD GUAYANA, BOLIVAR",\
                "CONTRIBUYENTE ORDINARIO"]""";
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 0, dataS);

        assertFalse(ToolsMqttResponseParser.isWifiScanResponse(item));
        assertTrue(ToolsMqttResponseParser.isHeaderFooterReadResponse(item));
    }

    @Test
    void parseHeaderFooterJoinsStringArrayLines() {
        String dataS = """
                ["CALLE PRINCIPAL","CIUDAD GUAYANA, BOLIVAR"]""";
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 0, dataS);

        assertEquals(
                "CALLE PRINCIPAL\nCIUDAD GUAYANA, BOLIVAR",
                parser.parseHeaderFooter(item));
    }

    @Test
    void parseHeaderFooterReturnsEmptyForMissingFooterMarker() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem(
                "StaInf", 0, 0, "SIN PIE DE TICKET FIJOS");

        assertEquals("", parser.parseHeaderFooter(item));
    }
}
