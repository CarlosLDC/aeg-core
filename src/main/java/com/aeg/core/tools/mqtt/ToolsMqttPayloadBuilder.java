package com.aeg.core.tools.mqtt;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.fiscal.FiscalTicketLatin2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ToolsMqttPayloadBuilder {

    private final ObjectMapper objectMapper;

    public ToolsMqttPayloadBuilder(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String statusPayload() {
        return staInfPayload(ToolsMqttConstants.STA_CONEXION_SIN_DNF);
    }

    public String wifiScanPayload() {
        return staInfPayload(ToolsMqttConstants.STA_GET_ACC_POI);
    }

    public String lastTransmittedZPayload() {
        return staInfPayload(ToolsMqttConstants.STA_ULT_Z_TX_SENI);
    }

    public String formasPagoReadPayload() {
        return staInfPayload(ToolsMqttConstants.STA_MEDIOS_PAGOS);
    }

    public String headerReadPayload() {
        return staInfPayload(ToolsMqttConstants.STA_ENC_FIJ);
    }

    public String footerReadPayload() {
        return staInfPayload(ToolsMqttConstants.STA_PIE_FIJ);
    }

    public String wifiConnectPayload(String ssid, String password) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_WIFI_CONF,
                "data", java.util.Map.of("ssid", ssid, "pass", password)));
    }

    public String wifiResetPayload() {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_RESET_MF,
                "data", ToolsMqttConstants.RESET_MF_DATA));
    }

    public String listReportZPayload() {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_GET_REP_Z,
                "data", -1));
    }

    public String generateReportZPayload() {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_REP_Z,
                "data", 0));
    }

    public String getReportZPayload(int reportNumber) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_GET_REP_Z,
                "data", reportNumber));
    }

    public String reportXPayload() {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_IMP_REP_X));
    }

    public String formasPagoWritePayload(int nroFp, String descripcion) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_DESC_FP,
                "data", java.util.Map.of("nroFP", nroFp, "descripcion", descripcion)));
    }

    public String headerWritePayload(String content) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_W_FILE_SPIFF,
                "data", FiscalTicketLatin2.normalizeFiscalTicketText(content)));
    }

    public String footerWritePayload(String content) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_PIE_TI_F,
                "data", FiscalTicketLatin2.normalizeFiscalTicketText(content)));
    }

    public String reprintPayload(String tipoRe, int number, boolean printPhysically) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_REIM_REP,
                "data", java.util.Map.of(
                        "tipoRe", tipoRe,
                        "nroReg", java.util.List.of(number),
                        "impFis", printPhysically ? 1 : 0)));
    }

    public String reprintPayload(String tipoRe, int number) {
        return reprintPayload(tipoRe, number, false);
    }

    private String staInfPayload(String status) {
        return writeJson(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_STA_INF,
                "data", java.util.Map.of("status", status)));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new EnajenacionProtocolException("Failed to serialize MQTT payload: " + ex.getMessage());
        }
    }
}
