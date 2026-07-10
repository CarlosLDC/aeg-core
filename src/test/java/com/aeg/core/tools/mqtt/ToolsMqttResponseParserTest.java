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
    void parseStatusReturnsSeniatOnlineForObjectDataS() {
        String dataS = """
                {"ConexionWifi":"AP_IoT_Home_CANTV","direccionIP":"192.168.1.101",\
                "EstatusSeniat":"EN LINEA","NroUltZEmit":2,"NroUltZTx":0,"DiasSinTx":0}""";
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 0, 1, dataS);

        ToolsMqttStatusResponse response = parser.parseStatus(item);

        assertTrue(response.success());
        assertEquals("EN LINEA", response.seniatStatus());
        assertEquals("AP_IoT_Home_CANTV", response.additionalInfo().wifiNetwork());
        assertEquals("192.168.1.101", response.additionalInfo().ipAddress());
        assertEquals(2, response.additionalInfo().lastZReport());
        assertTrue(ToolsMqttResponseParser.isStatusResponse(item));
    }

    @Test
    void parseStatusNormalizesEnLineaVariants() {
        assertEquals("EN LINEA", ToolsMqttResponseParser.normalizeSeniatStatus("EN  LINEA"));
        assertEquals("EN LINEA", ToolsMqttResponseParser.normalizeSeniatStatus("en linea"));
        assertEquals("SIN CONEXION", ToolsMqttResponseParser.normalizeSeniatStatus("OFFLINE"));
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
  void parseStatusReturnsSeniatOfflineWithNetworkInfoEvenWhenCodeIsNonZero() {
        String dataS = """
                {"EstatusSeniat":"SIN CONEXION","ConexionWifi":"AP_IoT_HomeP",\
                "direccionIP":"192.168.68.120","NroUltZEmit":2,"NroUltZTx":0,"DiasSinTx":1}""";
        FiscalMqttResponseItem item = new FiscalMqttResponseItem("StaInf", 20, null, dataS);

        ToolsMqttStatusResponse response = parser.parseStatus(item);

        assertTrue(response.success());
        assertEquals("SIN CONEXION", response.seniatStatus());
        assertEquals("AP_IoT_HomeP", response.additionalInfo().wifiNetwork());
        assertEquals("192.168.68.120", response.additionalInfo().ipAddress());
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
    void parseWifiScanUsesQosWhenRssiMissing() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem(
                "StaInf",
                0,
                null,
                "[{\"ssid\":\"AP_IoT_HomeP\",\"qos\":100},{\"ssid\":\"KENMARY\",\"qos\":32}]");

        var networks = parser.parseWifiScan(item);

        assertEquals(2, networks.size());
        assertEquals("AP_IoT_HomeP", networks.get(0).ssid());
        assertEquals(100, networks.get(0).signal());
        assertEquals("KENMARY", networks.get(1).ssid());
        assertEquals(32, networks.get(1).signal());
    }

    @Test
    void parseWifiScanDeduplicatesSsidKeepingBestSignal() {
        FiscalMqttResponseItem item = new FiscalMqttResponseItem(
                "StaInf",
                0,
                null,
                """
                [{"ssid":"AP_IoT_HomeP","qos":44},{"ssid":"AP_IoT_HomeP","qos":100}]\
                """);

        var networks = parser.parseWifiScan(item);

        assertEquals(1, networks.size());
        assertEquals("AP_IoT_HomeP", networks.get(0).ssid());
        assertEquals(100, networks.get(0).signal());
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
