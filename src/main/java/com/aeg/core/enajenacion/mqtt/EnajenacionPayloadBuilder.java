package com.aeg.core.enajenacion.mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.aeg.core.fiscal.FiscalInvoiceProductDescription;
import com.aeg.core.fiscal.FiscalTicketLatin2;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EnajenacionPayloadBuilder {

    private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;
    private final List<String> ticketFooterLines;
    private volatile String configSpiffsTemplate;

    public EnajenacionPayloadBuilder(
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper,
            @Value("${app.mqtt.enajenacion.ticket-footer-lines:}") String ticketFooterLines) {
        this.objectMapper = objectMapper;
        this.ticketFooterLines = parseTicketFooterLines(ticketFooterLines);
    }

    public String buildDnfAlertPayload() {
        List<Map<String, String>> commands = List.of(
                line("aperDNF", "DOCUMENTO NO FISCAL"),
                line("efeNeDAnJuCeDNF", "***** ALERTA *****"),
                line("efeNoDAnJuCeDNF", "IMPRESORA EN PROCESO"),
                line("efeNoDAnJuCeDNF", "DE ENAJENACION"),
                line("efeNoDAnJuCeDNF", "DURANTE EL PROCESO"),
                line("efeNoDAnJuCeDNF", "DE ENAJENACION"),
                line("efeNoDAnJuCeDNF", "NO PUEDE SER UTILIZADA,"),
                line("efeNoDAnJuCeDNF", "DEBE MANTENERSE ENCENDIDA."),
                line("efeNoDAnJuCeDNF", "EL PROCESO TERMINA CUANDO SE"),
                line("efeNoDAnJuCeDNF", "IMPRIMA UN REPORTE Z"),
                line("endDNF", "TIEMPO APROXIMADO DE ESPERA 3 MIN"));
        return writeJson(commands);
    }

    public String buildFiscalRifPayload(EnajenacionContext context) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("tituloSeniat", "SENIAT");
        contenido.put("rifEmp", RifFormatter.toFiscalForm(context.rif()));
        contenido.put("nomEmp", context.businessName());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nameFile", "rifEmp.json");
        data.put("Access", "config");
        data.put("contenido", contenido);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cmd", EnajenacionConstants.CMD_FISCAL_AEG);
        root.put("data", data);
        return writeJson(root);
    }

    public String buildHeaderPayload(EnajenacionContext context) {
        List<String> encFacFijo = List.copyOf(context.encFacFijoLines());
        List<String> pieFacFijo = new ArrayList<>(context.pieFacFijoLines());
        if (pieFacFijo.isEmpty() && !ticketFooterLines.isEmpty()) {
            pieFacFijo.addAll(ticketFooterLines);
        }

        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("encFacFijo", encFacFijo);
        if (!pieFacFijo.isEmpty()) {
            contenido.put("pieFacFijo", pieFacFijo);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("Access", "AeG-1968-2024");
        data.put("nameFile", "paramFacSPIFF.json");
        data.put("contenido", contenido);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cmd", EnajenacionConstants.CMD_W_FILE_SPIFF);
        root.put("data", data);
        return writeJson(root);
    }

    public String buildConfigSpiffsPayload() {
        return loadConfigSpiffsTemplate();
    }

    public static final String DEFAULT_INVOICE_PRODUCT_DESCRIPTION = "PRODUCTO";

    public String buildInvoicePayload() {
        return buildInvoicePayload(DEFAULT_INVOICE_PRODUCT_DESCRIPTION);
    }

    public String buildInvoicePayload(String productDescription) {
        List<String> descriptionLines = FiscalInvoiceProductDescription.resolveLines(
                productDescription,
                DEFAULT_INVOICE_PRODUCT_DESCRIPTION);
        List<String> profDescriptions = FiscalInvoiceProductDescription.linesForProfCommands(descriptionLines);
        List<Object> commands = new ArrayList<>();
        for (int imp = 1; imp <= 5; imp++) {
            commands.add(productLine("proF", imp, profDescriptions.get(imp - 1)));
        }
        commands.add(Map.of("cmd", "subToF", "data", 1, "valor", 0));
        commands.add(Map.of(
                "cmd", "fpaF",
                "data", Map.of("tipo", 1, "monto", -1, "tasaConv", 0)));
        commands.add(Map.of("cmd", "endFac", "data", 1));
        return writeJson(commands);
    }

    public String buildCreditNotePayload(EnajenacionContext context, int invoiceNumber, LocalDate invoiceDate) {
        List<Object> commands = new ArrayList<>();
        commands.add(Map.of("cmd", "nroFacNC", "data", invoiceNumber));
        commands.add(Map.of("cmd", "fechFacNC", "data", INVOICE_DATE.format(invoiceDate)));
        commands.add(Map.of("cmd", "conSerNC", "data", context.fiscalSerial()));
        commands.add(Map.of("cmd", "rifCiNC", "data", RifFormatter.toFiscalForm(context.rif())));
        commands.add(Map.of("cmd", "razSocNC", "data", splitBusinessName(context.businessName())));
        for (int imp = 1; imp <= 5; imp++) {
            commands.add(productLine("prodNC", imp, DEFAULT_INVOICE_PRODUCT_DESCRIPTION));
        }
        commands.add(Map.of("cmd", "endPoNC", "data", 1, "valor", 0));
        commands.add(Map.of(
                "cmd", "fpaNC",
                "data", Map.of("tipo", 1, "monto", -1, "tasaConv", 0)));
        commands.add(Map.of("cmd", "endNC", "data", 1));
        return writeJson(commands);
    }

    public String buildReportZPayload() {
        return writeJson(Map.of("cmd", EnajenacionConstants.CMD_GEN_IMP_REP_Z, "data", 1));
    }

    public String buildRegistrationStatusPayload() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cmd", EnajenacionConstants.CMD_STA_INF);
        root.put("data", Map.of("status", EnajenacionConstants.STA_INF_STATUS_NRO_REG_MA));
        return writeJson(root);
    }

    private static Map<String, Object> productLine(String cmd, int imp, String description) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pre", 100);
        data.put("cant", 1000);
        data.put("imp", imp);
        data.put("des01", description);
        return Map.of("cmd", cmd, "data", data);
    }

    static String normalizeProductDescription(String productDescription) {
        return String.join(
                "\n",
                FiscalInvoiceProductDescription.resolveLines(
                        productDescription,
                        DEFAULT_INVOICE_PRODUCT_DESCRIPTION));
    }

    private static Map<String, String> line(String cmd, String data) {
        return Map.of("cmd", cmd, "data", data);
    }

    static List<String> parseTicketFooterLines(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split("\\|", -1))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    static List<String> splitBusinessName(String businessName) {
        if (businessName == null || businessName.isBlank()) {
            return List.of("");
        }
        String trimmed = businessName.trim();
        if (trimmed.length() <= 45) {
            return List.of(trimmed);
        }
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < trimmed.length()) {
            int end = Math.min(start + 45, trimmed.length());
            if (end < trimmed.length()) {
                int space = trimmed.lastIndexOf(' ', end);
                if (space > start) {
                    end = space;
                }
            }
            lines.add(trimmed.substring(start, end).trim());
            start = end;
            while (start < trimmed.length() && trimmed.charAt(start) == ' ') {
                start++;
            }
        }
        return lines;
    }

    static List<String> buildEncFacFijo(
            String addressLine1,
            String addressLine2,
            String cityStateLine,
            String contributorTypeLine) {
        List<String> lines = new ArrayList<>();
        lines.add(addressLine1);
        if (addressLine2 != null && !addressLine2.isBlank()) {
            lines.add(addressLine2);
        }
        lines.add(cityStateLine);
        lines.add(contributorTypeLine);
        return List.copyOf(lines);
    }

    static AddressLines splitAddress(String address) {
        if (address == null || address.isBlank()) {
            return new AddressLines("", "");
        }
        String trimmed = address.trim();
        int newline = trimmed.indexOf('\n');
        if (newline >= 0) {
            return new AddressLines(
                    trimmed.substring(0, newline).trim(),
                    trimmed.substring(newline + 1).trim());
        }
        if (trimmed.length() <= 45) {
            return new AddressLines(trimmed, "");
        }
        int split = trimmed.lastIndexOf(' ', 45);
        if (split <= 0) {
            split = 45;
        }
        return new AddressLines(trimmed.substring(0, split).trim(), trimmed.substring(split).trim());
    }

    record AddressLines(String line1, String line2) {
    }

    private String loadConfigSpiffsTemplate() {
        if (configSpiffsTemplate != null) {
            return configSpiffsTemplate;
        }
        synchronized (this) {
            if (configSpiffsTemplate != null) {
                return configSpiffsTemplate;
            }
            ClassPathResource resource = new ClassPathResource("fiscal/configSPIFFS.json");
            try (InputStream input = resource.getInputStream()) {
                configSpiffsTemplate = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                return configSpiffsTemplate;
            } catch (IOException ex) {
                throw new EnajenacionProtocolException("Failed to load configSPIFFS template: " + ex.getMessage());
            }
        }
    }

    private String writeJson(Object value) {
        try {
            Object normalized = FiscalTicketLatin2.normalizePayloadValue(value);
            return objectMapper.writeValueAsString(normalized);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new EnajenacionProtocolException("Failed to serialize MQTT payload: " + ex.getMessage());
        }
    }
}
