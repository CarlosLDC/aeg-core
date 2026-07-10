package com.aeg.core.tools.mqtt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.fiscal.FiscalTicketLatin2;
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
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ObjectMapper objectMapper;

    public ToolsMqttStatusResponse parseStatus(FiscalMqttResponseItem response) {
        if (response.dataS() != null && !response.dataS().isBlank()) {
            ToolsMqttStatusResponse parsed = tryParseStatusInfo(response.dataS());
            if (parsed != null) {
                return parsed;
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

    private ToolsMqttStatusResponse tryParseStatusInfo(String dataS) {
        try {
            JsonNode node = parseStaInfDataNode(dataS);
            if (node == null || !node.has("EstatusSeniat")) {
                return null;
            }
            String seniatStatus = normalizeSeniatStatus(node.path("EstatusSeniat").asText(""));
            ToolsMqttAdditionalInfoDto info = new ToolsMqttAdditionalInfoDto(
                    textOrNa(node, "ConexionWifi"),
                    textOrNa(node, "direccionIP"),
                    node.path("NroUltZEmit").asInt(0),
                    node.has("NroUltZTx") && !node.get("NroUltZTx").isNull()
                            ? node.get("NroUltZTx").asInt()
                            : null,
                    node.path("DiasSinTx").asInt(0));
            return ToolsMqttStatusResponse.ok(seniatStatus, info);
        } catch (Exception ignored) {
            return null;
        }
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
            LinkedHashMap<String, ToolsWifiNetworkDto> networksBySsid = new LinkedHashMap<>();
            for (Map<String, Object> entry : raw) {
                Object ssid = entry.get("ssid");
                if (ssid == null || ssid.toString().isBlank()) {
                    continue;
                }
                Integer signal = readWifiSignal(entry);
                String ssidValue = ssid.toString();
                ToolsWifiNetworkDto existing = networksBySsid.get(ssidValue);
                if (existing == null || compareWifiSignal(signal, existing.signal()) > 0) {
                    networksBySsid.put(ssidValue, new ToolsWifiNetworkDto(ssidValue, signal));
                }
            }
            return new ArrayList<>(networksBySsid.values());
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
        String dataS = response.dataS().trim();
        if (dataS.isBlank()) {
            return "";
        }
        if ("SIN PIE DE TICKET FIJOS".equalsIgnoreCase(dataS)) {
            return "";
        }
        if (isJsonArrayOfStrings(dataS)) {
            try {
                List<String> lines = objectMapper.readValue(dataS, new TypeReference<>() {});
                return String.join("\n", FiscalTicketLatin2.normalizeFiscalTicketLines(lines));
            } catch (Exception ex) {
                throw new ToolsMqttOperationException("No se pudo interpretar el encabezado o pie de página.");
            }
        }
        return FiscalTicketLatin2.normalizeFiscalTicketText(response.dataS());
    }

    public String parseReprintChunks(List<String> chunks) {
        return String.join("", chunks).trim();
    }

    public static boolean isStatusResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        if (item.dataS() == null || item.dataS().isBlank()) {
            return false;
        }
        if (item.dataS().contains("EstatusSeniat")) {
            return true;
        }
        try {
            JsonNode node = JSON.readTree(item.dataS());
            if (node.isTextual()) {
                String inner = node.asText().trim();
                if (inner.startsWith("{")) {
                    node = JSON.readTree(inner);
                }
            }
            return node.isObject() && node.has("EstatusSeniat");
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isWifiScanResponse(FiscalMqttResponseItem item) {
        if (item == null || !ToolsMqttConstants.CMD_STA_INF.equalsIgnoreCase(item.cmd())) {
            return false;
        }
        if (item.dataS() == null || item.dataS().isBlank()) {
            return false;
        }
        return isJsonArrayOfObjectsWithField(item.dataS(), "ssid");
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
        if (isStatusResponse(item)
                || isWifiScanResponse(item)
                || isFormasPagoResponse(item)
                || isLastTransmittedZResponse(item)) {
            return false;
        }
        String trimmed = item.dataS().trim();
        return isJsonArrayOfStrings(trimmed)
                || "SIN PIE DE TICKET FIJOS".equalsIgnoreCase(trimmed);
    }

    private static Integer readWifiSignal(Map<String, Object> entry) {
        Object rssi = entry.get("rssi");
        if (rssi instanceof Number number) {
            return number.intValue();
        }
        Object qos = entry.get("qos");
        if (qos instanceof Number qosNumber) {
            return qosNumber.intValue();
        }
        return null;
    }

    private static int compareWifiSignal(Integer left, Integer right) {
        int leftValue = left != null ? left : Integer.MIN_VALUE;
        int rightValue = right != null ? right : Integer.MIN_VALUE;
        return Integer.compare(leftValue, rightValue);
    }

    private static String textOrNa(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? "N/A" : value;
    }

    private JsonNode parseStaInfDataNode(String dataS) throws Exception {
        JsonNode node = objectMapper.readTree(dataS);
        if (node.isTextual()) {
            String inner = node.asText().trim();
            if (inner.startsWith("{") || inner.startsWith("[")) {
                return objectMapper.readTree(inner);
            }
        }
        return node;
    }

    static String normalizeSeniatStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "SIN CONEXION";
        }
        String normalized = raw.trim().replaceAll("\\s+", " ");
        if ("EN LINEA".equalsIgnoreCase(normalized)) {
            return "EN LINEA";
        }
        String upper = normalized.toUpperCase();
        if (upper.contains("EN LINEA") || upper.contains("ENLINEA")) {
            return "EN LINEA";
        }
        return "SIN CONEXION";
    }

    private static boolean isHexValue(String value) {
        return value != null && value.trim().matches("^[0-9A-Fa-f]+$");
    }

    private static boolean isJsonArrayOfStrings(String value) {
        try {
            JsonNode node = JSON.readTree(value);
            if (!node.isArray() || node.isEmpty()) {
                return false;
            }
            for (JsonNode element : node) {
                if (!element.isTextual()) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isJsonArrayOfObjectsWithField(String value, String field) {
        try {
            JsonNode node = JSON.readTree(value);
            if (!node.isArray() || node.isEmpty()) {
                return false;
            }
            for (JsonNode element : node) {
                if (!element.isObject() || !element.has(field)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
