package com.aeg.core.tools.mqtt;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.aeg.core.enajenacion.mqtt.EnajenacionProtocolException;
import com.aeg.core.enajenacion.mqtt.FiscalMqttSyncResponseAwaiter;
import com.aeg.core.enajenacion.mqtt.FiscalMqttTopics;
import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerConfigResult;
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerConfigResult.Outcome;
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerResult;
import com.aeg.core.mqtt.dto.BrokerMigrationFirmwareResult;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.tools.mqtt.BrokerMigrationLegacyClient.LegacyBrokerTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Protocolo de migración de una impresora AEG del broker antiguo ({@value ToolsMqttConstants#LEGACY_BROKER_HOST})
 * al broker actual ({@value ToolsMqttConstants#NEW_BROKER_HOST}):
 * <ol>
 *   <li>{@code statFirmSinDNF}: 1.1.0 → requiere {@code updFirmDown}; 1.1.1 → firmware ya actualizado.</li>
 *   <li>{@code updFirmDown} (solo por broker antiguo) y esperar reinicio.</li>
 *   <li>{@code StaInf/EstCredMqtt1}: identifica a qué broker está conectada.</li>
 *   <li>{@code configMqttAEG} (por broker antiguo) con las credenciales del broker nuevo.</li>
 * </ol>
 */
@Slf4j
@Service
public class BrokerMigrationService {

    static final int LEGACY_TIMEOUT_SECONDS = 20;
    static final int NEW_BROKER_TIMEOUT_SECONDS = 15;
    static final int UPD_FIRM_TIMEOUT_SECONDS = 30;
    static final int VERIFY_ATTEMPTS = 4;
    static final int VERIFY_ATTEMPT_TIMEOUT_SECONDS = 10;
    static final long VERIFY_INITIAL_DELAY_MS = 5_000;

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)+)\\s*$");

    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;
    private final BrokerMigrationLegacyClient legacyClient;
    private final MqttService mqttService;
    private final FiscalMqttSyncResponseAwaiter syncResponseAwaiter;
    private final ObjectMapper objectMapper;

    public BrokerMigrationService(
            PrinterRepository printerRepository,
            SecurityScopeService securityScope,
            BrokerMigrationLegacyClient legacyClient,
            MqttService mqttService,
            FiscalMqttSyncResponseAwaiter syncResponseAwaiter,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
        this.legacyClient = legacyClient;
        this.mqttService = mqttService;
        this.syncResponseAwaiter = syncResponseAwaiter;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- 1. Firmware

    public BrokerMigrationFirmwareResult checkFirmware(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        log.info("Checking firmware for printer {}", printerId);

        BrokerReply reply = requestOnAnyBroker(ctx, firmwareStatusPayload(), ToolsMqttConstants.CMD_STAT_FIRM_SIN_DNF);
        requireCodeZero(reply.node(), ToolsMqttConstants.CMD_STAT_FIRM_SIN_DNF);

        String dataS = reply.node().path("dataS").asText("").trim();
        String version = extractVersion(dataS);

        if (ToolsMqttConstants.FIRMWARE_VERSION_LEGACY.equals(version)) {
            return new BrokerMigrationFirmwareResult(version, true);
        }
        if (compareVersions(version, ToolsMqttConstants.FIRMWARE_VERSION_MIGRATED) >= 0) {
            return new BrokerMigrationFirmwareResult(version, false);
        }
        throw new EnajenacionProtocolException(
                "Versión de firmware " + version + " no contemplada por el protocolo de migración (se esperaba "
                        + ToolsMqttConstants.FIRMWARE_VERSION_LEGACY + " o "
                        + ToolsMqttConstants.FIRMWARE_VERSION_MIGRATED + ").");
    }

    static String extractVersion(String dataS) {
        Matcher matcher = VERSION_PATTERN.matcher(dataS == null ? "" : dataS.trim());
        if (!matcher.find()) {
            throw new EnajenacionProtocolException(
                    "No se pudo leer la versión de firmware en la respuesta: \"" + dataS + "\"");
        }
        return matcher.group(1);
    }

    static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length ? Integer.parseInt(pa[i]) : 0;
            int vb = i < pb.length ? Integer.parseInt(pb[i]) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    /**
     * Envía {@code updFirmDown} por el broker antiguo. Si la impresora no confirma (puede reiniciarse
     * sin ACK) se devuelve éxito con aviso: el cliente debe volver a ejecutar {@link #checkFirmware}
     * tras el reinicio para confirmar la versión {@value ToolsMqttConstants#FIRMWARE_VERSION_MIGRATED}.
     */
    public ToolsMqttSimpleResponse triggerFirmwareUpdate(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        log.info("Triggering firmware update for printer {}", printerId);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cmd", ToolsMqttConstants.CMD_UPD_FIRM_DOWN);
        payload.put("data", ToolsMqttConstants.UPD_FIRM_DOWN_DATA);

        String responseJson;
        try {
            responseJson = legacyClient.publishAndAwait(
                    ctx.compactMac(), toJson(payload), ToolsMqttConstants.CMD_UPD_FIRM_DOWN, UPD_FIRM_TIMEOUT_SECONDS);
        } catch (LegacyBrokerTimeoutException e) {
            log.warn("No ACK for updFirmDown on printer {}; will verify firmware after reboot", printerId);
            return ToolsMqttSimpleResponse.ok(
                    "No se recibió confirmación (la impresora puede estar reiniciándose). "
                            + "La versión se verificará tras el reinicio.");
        }

        JsonNode node = readJson(responseJson);
        int code = node.path("code").asInt(-1);
        if (code == 0) {
            return ToolsMqttSimpleResponse.ok("Actualización de firmware iniciada.");
        }
        return ToolsMqttSimpleResponse.error("La impresora rechazó la actualización de firmware (code=" + code + ").");
    }

    // ---------------------------------------------------------------- 2. Broker actual

    public BrokerMigrationBrokerResult checkBroker(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        log.info("Checking broker for printer {}", printerId);

        BrokerReply reply = requestOnAnyBroker(ctx, brokerStatusPayload(), ToolsMqttConstants.CMD_STA_INF);
        requireCodeZero(reply.node(), ToolsMqttConstants.CMD_STA_INF);

        String host = dataSObject(reply.node()).path("host").asText("");
        // Si respondió por el broker antiguo, está conectada a él por definición (independiente del host reportado).
        boolean isOldBroker = reply.viaLegacy() || ToolsMqttConstants.LEGACY_BROKER_HOST.equals(host);
        return new BrokerMigrationBrokerResult(host, isOldBroker);
    }

    // ---------------------------------------------------------------- 3. Cambio de credenciales

    public BrokerMigrationBrokerConfigResult configureBroker(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        log.info("Configuring new broker for printer {}", printerId);
        return configureBroker(ctx);
    }

    /**
     * Variante para impresoras nuevas (aún no registradas en BD), usada por el asistente de
     * fiscalización USB como fallback cuando el firmware no acepta {@code configMqttAEG} por USB.
     */
    public BrokerMigrationBrokerConfigResult configureBrokerByMac(String mac) {
        String compactMac = MacAddressNormalizer.requireCompactForm(mac);
        log.info("Configuring new broker for unregistered printer mac={}", compactMac);
        return configureBroker(new PrinterContext(compactMac, FiscalMqttTopics.comandoTopic(compactMac)));
    }

    private BrokerMigrationBrokerConfigResult configureBroker(PrinterContext ctx) {
        try {
            String responseJson = legacyClient.publishAndAwait(
                    ctx.compactMac(), configMqttPayload(), ToolsMqttConstants.CMD_CONFIG_MQTT_AEG, LEGACY_TIMEOUT_SECONDS);
            int code = readJson(responseJson).path("code").asInt(-1);
            if (code == 0) {
                return new BrokerMigrationBrokerConfigResult(
                        Outcome.MIGRATED, "Credenciales del broker nuevo aceptadas (ACK recibido).");
            }
            return new BrokerMigrationBrokerConfigResult(
                    Outcome.REJECTED, "La impresora rechazó la configuración (code=" + code + ").");
        } catch (EnajenacionProtocolException e) {
            log.warn("No configMqttAEG ACK for printer {} ({}); verifying on new broker", ctx.compactMac(), e.getMessage());
        }

        if (verifyOnNewBroker(ctx)) {
            return new BrokerMigrationBrokerConfigResult(
                    Outcome.MIGRATED, "ACK perdido, migración confirmada en el broker nuevo.");
        }
        return new BrokerMigrationBrokerConfigResult(
                Outcome.UNCERTAIN,
                "No se pudo confirmar. Verifique la impresora e intente verificar el broker nuevamente.");
    }

    private boolean verifyOnNewBroker(PrinterContext ctx) {
        pause(VERIFY_INITIAL_DELAY_MS);
        for (int attempt = 1; attempt <= VERIFY_ATTEMPTS; attempt++) {
            try {
                JsonNode node = requestOnNewBroker(
                        ctx, brokerStatusPayload(), ToolsMqttConstants.CMD_STA_INF, VERIFY_ATTEMPT_TIMEOUT_SECONDS);
                String host = dataSObject(node).path("host").asText("");
                log.info("Printer {} answered on new broker (host={}) attempt {}", ctx.compactMac(), host, attempt);
                // Responder en el broker nuevo confirma por sí mismo que la migración se aplicó.
                return true;
            } catch (EnajenacionProtocolException ex) {
                log.debug("Verify attempt {}/{} failed for {}: {}", attempt, VERIFY_ATTEMPTS, ctx.compactMac(), ex.getMessage());
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- Transporte

    private record BrokerReply(JsonNode node, boolean viaLegacy) {}

    /** Intenta primero el broker antiguo y, si la impresora no responde allí, el broker nuevo. */
    private BrokerReply requestOnAnyBroker(PrinterContext ctx, String payload, String expectedCmd) {
        try {
            String json = legacyClient.publishAndAwait(ctx.compactMac(), payload, expectedCmd, LEGACY_TIMEOUT_SECONDS);
            return new BrokerReply(readJson(json), true);
        } catch (EnajenacionProtocolException legacyEx) {
            log.info("No {} reply on legacy broker for {} ({}); trying new broker",
                    expectedCmd, ctx.compactMac(), legacyEx.getMessage());
        }
        try {
            return new BrokerReply(requestOnNewBroker(ctx, payload, expectedCmd, NEW_BROKER_TIMEOUT_SECONDS), false);
        } catch (EnajenacionProtocolException newEx) {
            throw new EnajenacionProtocolException("La impresora no respondió en ningún broker.");
        }
    }

    private JsonNode requestOnNewBroker(PrinterContext ctx, String payload, String expectedCmd, int timeoutSeconds) {
        CompletableFuture<FiscalMqttResponseItem> future = syncResponseAwaiter.registerWithMatcher(
                ctx.compactMac(),
                item -> item.cmd() != null && item.cmd().trim().equalsIgnoreCase(expectedCmd));
        try {
            mqttService.publish(ctx.topic(), payload);
            FiscalMqttResponseItem item = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return toNode(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnajenacionProtocolException("Operación interrumpida esperando al broker nuevo.");
        } catch (Exception e) {
            throw new EnajenacionProtocolException("La impresora no respondió en el broker nuevo.");
        } finally {
            // Solo liberar si sigue pendiente: si ya se completó, el awaiter la removió y podría
            // haber otra espera registrada para la misma MAC.
            if (!future.isDone()) {
                syncResponseAwaiter.cancel(ctx.compactMac());
            }
        }
    }

    private JsonNode toNode(FiscalMqttResponseItem item) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("cmd", item.cmd());
        if (item.code() != null) {
            node.put("code", item.code());
        }
        if (item.dataD() != null) {
            node.put("dataD", item.dataD());
        }
        if (item.dataS() != null) {
            node.put("dataS", item.dataS());
        }
        return node;
    }

    // ---------------------------------------------------------------- Helpers

    /** {@code dataS} puede llegar como objeto JSON o como string con JSON dentro. */
    private JsonNode dataSObject(JsonNode response) {
        JsonNode dataS = response.path("dataS");
        if (dataS.isTextual()) {
            String text = dataS.asText().trim();
            if (text.startsWith("{")) {
                try {
                    return objectMapper.readTree(text);
                } catch (Exception e) {
                    log.warn("dataS is not valid JSON: {}", text);
                }
            }
            return objectMapper.createObjectNode();
        }
        return dataS.isObject() ? dataS : objectMapper.createObjectNode();
    }

    private static void requireCodeZero(JsonNode node, String cmd) {
        int code = node.path("code").asInt(-1);
        if (code != 0) {
            throw new EnajenacionProtocolException("La impresora respondió " + cmd + " con code=" + code + ".");
        }
    }

    private String firmwareStatusPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cmd", ToolsMqttConstants.CMD_STAT_FIRM_SIN_DNF);
        payload.put("data", 0);
        return toJson(payload);
    }

    private String brokerStatusPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cmd", ToolsMqttConstants.CMD_STA_INF);
        payload.putObject("data").put("status", ToolsMqttConstants.STA_EST_CRED_MQTT1);
        return toJson(payload);
    }

    public String configMqttPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("cmd", ToolsMqttConstants.CMD_CONFIG_MQTT_AEG);
        ObjectNode data = payload.putObject("data");
        data.put("addrBroker", ToolsMqttConstants.NEW_BROKER_HOST);
        data.put("portBroker", ToolsMqttConstants.NEW_BROKER_PORT);
        data.put("userBroker", ToolsMqttConstants.NEW_BROKER_USER);
        data.put("passwBroker", ToolsMqttConstants.NEW_BROKER_PASSWORD);
        data.put("topicRx", ToolsMqttConstants.CONFIG_MQTT_TOPIC_RX);
        data.put("topicTx", ToolsMqttConstants.CONFIG_MQTT_TOPIC_TX);
        data.put("topicDoc", ToolsMqttConstants.CONFIG_MQTT_TOPIC_DOC);
        return toJson(payload);
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize MQTT payload", e);
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new EnajenacionProtocolException("Respuesta de la impresora no es JSON válido: " + json);
        }
    }

    /** Extraído para poder anularlo en tests. */
    void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private PrinterContext resolvePrinter(Long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        securityScope.assertPrinterInScope(printer);
        if (printer.getMacAddress() == null || printer.getMacAddress().isBlank()) {
            throw new EnajenacionProtocolException(
                    "La impresora no tiene dirección MAC registrada. Regístrela antes de usar operaciones MQTT.");
        }
        String compactMac = MacAddressNormalizer.requireCompactForm(printer.getMacAddress());
        return new PrinterContext(compactMac, FiscalMqttTopics.comandoTopic(compactMac));
    }

    private record PrinterContext(String compactMac, String topic) {}
}
