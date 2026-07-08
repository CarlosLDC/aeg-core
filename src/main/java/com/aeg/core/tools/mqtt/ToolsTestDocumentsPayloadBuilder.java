package com.aeg.core.tools.mqtt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ToolsTestDocumentsPayloadBuilder {

    private static final Map<String, Object> TEST_PRODUCT = Map.of(
            "pre", 100,
            "cant", 1000,
            "imp", 1,
            "des01", ToolsMqttConstants.TEST_PRODUCT_DESCRIPTION);

    private final ObjectMapper objectMapper;

    public ToolsTestDocumentsPayloadBuilder(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<String> testInvoicePayloads() {
        List<String> payloads = new ArrayList<>();
        for (int taxRate = 1; taxRate <= 5; taxRate++) {
            payloads.add(command("proF", productLine(taxRate)));
        }
        payloads.add(writeJson(Map.of(
                "cmd", "subToF",
                "data", 1,
                "valor", 0)));
        payloads.add(command("fpaF", Map.of("tipo", 1, "monto", -1, "tasaConv", 0)));
        payloads.add(command(ToolsMqttConstants.CMD_END_FAC, 1));
        return payloads;
    }

    public List<String> testCreditNotePayloads(String printerSerial) {
        List<String> payloads = new ArrayList<>();
        payloads.add(command("nroFacNC", 1));
        payloads.add(command("fechFacNC", ToolsMqttConstants.TEST_NOTE_DATE));
        payloads.add(command("conSerNC", printerSerial));
        payloads.add(command("rifCiNC", " "));
        payloads.add(command("razSocNC", List.of(" ")));
        for (int taxRate = 1; taxRate <= 5; taxRate++) {
            payloads.add(command("prodNC", productLine(taxRate)));
        }
        payloads.add(writeJson(Map.of(
                "cmd", "endPoNC",
                "data", 1,
                "valor", 0)));
        payloads.add(command("fpaNC", Map.of("tipo", 1, "monto", -1, "tasaConv", 0)));
        payloads.add(command(ToolsMqttConstants.CMD_END_NC, 1));
        return payloads;
    }

    public List<String> testDebitNotePayloads(String printerSerial) {
        List<String> payloads = new ArrayList<>();
        payloads.add(command("nroFacND", 1));
        payloads.add(command("fechFacND", ToolsMqttConstants.TEST_NOTE_DATE));
        payloads.add(command("conSerND", printerSerial));
        payloads.add(command("rifCiND", " "));
        payloads.add(command("razSocND", List.of(" ")));
        for (int taxRate = 1; taxRate <= 5; taxRate++) {
            payloads.add(command("prodND", productLine(taxRate)));
        }
        payloads.add(writeJson(Map.of(
                "cmd", "endPoND",
                "data", 1,
                "valor", 0)));
        payloads.add(command("fpaND", Map.of("tipo", 1, "monto", -1, "tasaConv", 0)));
        payloads.add(command(ToolsMqttConstants.CMD_END_ND, 1));
        return payloads;
    }

    public List<String> testGenerateZPayloads() {
        return List.of(command(ToolsMqttConstants.CMD_GEN_IMP_REP_Z, 1));
    }

    private Map<String, Object> productLine(int taxRate) {
        Map<String, Object> line = new LinkedHashMap<>(TEST_PRODUCT);
        line.put("imp", taxRate);
        return line;
    }

    private String command(String cmd, Object data) {
        return writeJson(Map.of("cmd", cmd, "data", data));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new EnajenacionProtocolException("Failed to serialize test document payload: " + ex.getMessage());
        }
    }
}
