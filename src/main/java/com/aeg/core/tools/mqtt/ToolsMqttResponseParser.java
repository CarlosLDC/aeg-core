package com.aeg.core.tools.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.dto.ToolsFormasPagoItemDto;
import com.aeg.core.mqtt.dto.ToolsMqttAdditionalInfoDto;
import com.aeg.core.mqtt.dto.ToolsMqttStatusResponse;
import com.aeg.core.mqtt.dto.ToolsReportZDataDto;
import com.aeg.core.mqtt.dto.ToolsTransmitZResponse;
import com.aeg.core.mqtt.dto.ToolsWifiNetworkDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ToolsMqttResponseParser {

    private static final Pattern FORMA_PAGO_KEY = Pattern.compile("^\\[(\\d+)\\](.+)$");

    private final ObjectMapper objectMapper;

    public ToolsMqttStatusResponse parseStatus(FiscalMqttResponseItem response) {
        if (response.code() != null && response.code() == 0 && response.dataS() != null) {
            try {
                JsonNode node = objectMapper.readTree(response.dataS());
                if (node.has("EstatusSeniat")) {
                    String seniatRaw = node.path("EstatusSeniat").asText("");
                    String seniatStatus = "EN LINEA".equalsIgnoreCase(seniatRaw.trim())
                            ? "EN LINEA"
                            : "SIN CONEXION";
                    ToolsMqttAdditionalInfoDto info = new ToolsMqttAdditionalInfoDto(
                            textOrNa(node, "ConexionWifi"),
                            textOrNa(node, "direccionIP"),
                            node.path("NroUltZEmit").asInt(0),
                            node.has("NroUltZTx") && !node.get("NroUltZTx").isNull()
                                    ? node.get("NroUltZTx").asInt()
                                    : null,
                            node.path("DiasSinTx").asInt(0));
                    return ToolsMqttStatusResponse.ok(seniatStatus, info);
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        if (response.dataS() != null && response.dataS().contains("Impresora")) {
            return ToolsMqttStatusResponse.ok("SIN CONEXION", null);
        }

        String errorMessage = response.dataS() != null && !response.dataS().isBlank()
                ? response.dataS()
                : "Error al consultar estado";
        return ToolsMqttStatusResponse.error(errorMessage, response.code());
    }

    public List<ToolsWifiNetworkDto> parseWifiScan(FiscalMqttResponseItem response) {
        if (response.code() != null && response.code() != 0) {
            throw new ToolsMqttOperationException(
                    "Error de impresora en escaneo WiFi (code: " + response.code() + ")");
        }
        if (response.dataS() == null || response.dataS().isBlank()) {
            throw new ToolsMqttOperationException("La impresora no devolvió redes WiFi.");
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(
                    response.dataS(), new TypeReference<>() {});
            List<ToolsWifiNetworkDto> networks = new ArrayList<>();
            for (Map<String, Object> entry : raw) {
                Object ssid = entry.get("ssid");
                if (ssid != null && !ssid.toString().isBlank()) {
                    Object rssi = entry.get("rssi");
                    Integer signal = rssi instanceof Number number ? number.intValue() : null;
                    networks.add(new ToolsWifiNetworkDto(ssid.toString(), signal));
                }
            }
            return networks;
        } catch (Exception ex) {
            throw new ToolsMqttOperationException("No se pudo interpretar la lista de redes WiFi.");
        }
    }

    public void parseWifiConnect(FiscalMqttResponseItem response) {
        if (response.code() != null && response.code() == 0 && (response.dataS() == null || response.dataS().isBlank())) {
            return;
        }
        String message = response.dataS() != null && !response.dataS().isBlank()
                ? response.dataS()
                : "Error de conexión WiFi (code: " + response.code() + ")";
        throw new ToolsMqttOperationException(message);
    }

    public ToolsReportZDataDto parseReportZ(FiscalMqttResponseItem response) {
        if (response.code() != null && response.code() != 0) {
            throw new ToolsMqttOperationException(
                    "Error de impresora en reporte Z (code: " + response.code() + ")");
        }
        if (response.dataS() == null || response.dataS().isBlank()) {
            throw new ToolsMqttOperationException("La impresora no devolvió datos del reporte Z.");
        }
        try {
            Map<String, Object> report = objectMapper.readValue(response.dataS(), new TypeReference<>() {});
            return new ToolsReportZDataDto(report);
        } catch (Exception ex) {
            throw new ToolsMqttOperationException("No se pudo interpretar el reporte Z.");
        }
    }

    public ToolsTransmitZResponse parseTransmitZ(FiscalMqttResponseItem response) {
        if (response.dataS() != null && isHexValue(response.dataS())) {
            try {
                int lastZ = Integer.parseInt(response.dataS().trim(), 16);
                return ToolsTransmitZResponse.ok(lastZ);
            } catch (NumberFormatException ex) {
                throw new ToolsMqttOperationException("Respuesta de transmisión Z inválida.");
            }
        }
        if (response.dataS() != null && !response.dataS().isBlank() && !response.dataS().contains("Impresora")) {
            return ToolsTransmitZResponse.seniatUnavailable("SENIAT no responde");
        }
        if (response.code() != null && response.code() != 0) {
            throw new ToolsMqttOperationException(
                    response.dataS() != null ? response.dataS() : "Error al transmitir reporte Z");
        }
        throw new ToolsMqttOperationException("No se recibió confirmación de transmisión Z.");
    }

    public List<ToolsFormasPagoItemDto> parseFormasPago(FiscalMqttResponseItem response) {
        if (response.code() == null || response.code() != 0 || response.dataS() == null) {
            throw new ToolsMqttOperationException(
                    "Error de impresora (code: " + response.code() + ")");
        }
        try {
            Map<String, String> raw = objectMapper.readValue(response.dataS(), new TypeReference<>() {});
            List<ToolsFormasPagoItemDto> items = new ArrayList<>();
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                Matcher matcher = FORMA_PAGO_KEY.matcher(entry.getKey());
                if (matcher.matches()) {
                    items.add(new ToolsFormasPagoItemDto(
                            Integer.parseInt(matcher.group(1)),
                            matcher.group(2)));
                }
            }
            return items;
        } catch (Exception ex) {
            throw new ToolsMqttOperationException("Error interpretando formas de pago.");
        }
    }

    public void parseSimpleAck(FiscalMqttResponseItem response, String defaultError) {
        if (response.code() != null && response.code() == 0 && (response.dataS() == null || response.dataS().isBlank())) {
            return;
        }
        String message = response.dataS() != null && !response.dataS().isBlank()
                ? response.dataS()
                : defaultError + " (code: " + response.code() + ")";
        throw new ToolsMqttOperationException(message);
    }

    public String parseHeaderFooter(FiscalMqttResponseItem response) {
        if (response.code() != null && response.code() != 0) {
            throw new ToolsMqttOperationException(
                    "Error de impresora (code: " + response.code() + ")");
        }
        if (response.dataS() == null) {
            return "";
        }
        return response.dataS();
    }

    public String parseReprintChunks(List<String> chunks) {
        return String.join("", chunks);
    }

    public static boolean isStatusResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        if (item.dataS() == null) {
            return false;
        }
        return item.dataS().contains("EstatusSeniat");
    }

    public static boolean isWifiScanResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        if (item.dataS() == null || item.dataS().isBlank()) {
            return false;
        }
        String trimmed = item.dataS().trim();
        return trimmed.startsWith("[");
    }

    public static boolean isFormasPagoResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        if (item.dataS() == null || item.dataS().isBlank()) {
            return false;
        }
        String trimmed = item.dataS().trim();
        return trimmed.startsWith("{") && trimmed.contains("[");
    }

    public static boolean isLastTransmittedZResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        return item.dataS() != null && isHexValue(item.dataS());
    }

    public static boolean isHeaderFooterReadResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        if (item.dataS() == null || item.dataS().isBlank()) {
            return false;
        }
        return !isStatusResponse(item)
                && !isWifiScanResponse(item)
                && !isFormasPagoResponse(item)
                && !isLastTransmittedZResponse(item);
    }

    private static String textOrNa(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? "N/A" : value;
    }

    private static boolean isHexValue(String value) {
        return value != null && value.trim().matches("^[0-9A-Fa-f]+$");
    }
}
