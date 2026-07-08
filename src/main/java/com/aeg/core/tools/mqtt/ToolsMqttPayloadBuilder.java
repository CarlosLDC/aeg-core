package com.aeg.core.tools.mqtt;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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

    public String wifiConnectPayload(String ssid, String password) throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_WIFI_CONF,
                "data", java.util.Map.of("ssid", ssid, "pass", password)));
    }

    public String wifiResetPayload() throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_RESET_MF,
                "data", ToolsMqttConstants.RESET_MF_DATA));
    }

    public String listReportZPayload() throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_GET_REP_Z,
                "data", -1));
    }

    public String generateReportZPayload() throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_REP_Z,
                "data", 0));
    }

    public String getReportZPayload(int reportNumber) throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_GET_REP_Z,
                "data", reportNumber));
    }

    public String reportXPayload() throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_IMP_REP_X));
    }

    public String formasPagoWritePayload(int nroFp, String descripcion) throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_DESC_FP,
                "data", java.util.Map.of("nroFP", nroFp, "descripcion", descripcion)));
    }

    public String headerWritePayload(String content) throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_W_FILE_SPIFF,
                "data", content));
    }

    public String footerWritePayload(String content) throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_PIE_TI_F,
                "data", content));
    }

    public String reprintPayload(String docType, int number) throws JsonProcessingException {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "cmd", ToolsMqttConstants.CMD_REIM_REP,
                "data", java.util.Map.of("tipo", docType, "nro", number)));
    }

    private String staInfPayload(String status) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "cmd", ToolsMqttConstants.CMD_STA_INF,
                    "data", java.util.Map.of("status", status)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build StaInf payload", ex);
        }
    }
}
