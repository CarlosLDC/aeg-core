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
import com.aeg.core.mqtt.dto.BrokerMigrationBrokerResult;
import com.aeg.core.mqtt.dto.BrokerMigrationFirmwareResult;
import com.aeg.core.mqtt.dto.ToolsMqttSimpleResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BrokerMigrationService {

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

    public BrokerMigrationFirmwareResult checkFirmware(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        String payload = "{\"cmd\":\"statFirmSinDNF\",\"data\":0}";
        log.info("Checking firmware for printer {}", printerId);
        
        String responseJson = legacyClient.publishAndAwait(ctx.compactMac(), payload, "statFirmSinDNF", 20);
        
        try {
            JsonNode node = objectMapper.readTree(responseJson);
            String dataS = node.has("dataS") ? node.get("dataS").asText() : "";
            String version = extractVersion(dataS);
            boolean needsUpdate = "1.1.0".equals(version);
            return new BrokerMigrationFirmwareResult(version, needsUpdate);
        } catch (Exception e) {
            log.error("Failed to parse firmware response", e);
            throw new EnajenacionProtocolException("Error parsing firmware response: " + e.getMessage());
        }
    }

    private String extractVersion(String dataS) {
        Pattern pattern = Pattern.compile("[\\d.]+$");
        Matcher matcher = pattern.matcher(dataS);
        if (matcher.find()) {
            return matcher.group();
        }
        return dataS;
    }

    public ToolsMqttSimpleResponse triggerFirmwareUpdate(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        String payload = "{\"cmd\":\"updFirmDown\",\"data\":1}";
        log.info("Triggering firmware update for printer {}", printerId);
        
        try {
            String responseJson = legacyClient.publishAndAwait(ctx.compactMac(), payload, "updFirmDown", 30);
            JsonNode node = objectMapper.readTree(responseJson);
            if (node.has("code") && node.get("code").asInt() == 0) {
                return ToolsMqttSimpleResponse.ok("Firmware update triggered successfully.");
            }
            return ToolsMqttSimpleResponse.error("Printer responded with error code.");
        } catch (Exception e) {
            log.error("Failed to trigger firmware update", e);
            return ToolsMqttSimpleResponse.error("Error triggering firmware update: " + e.getMessage());
        }
    }

    public BrokerMigrationBrokerResult checkBroker(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        String payload = "{\"cmd\":\"StaInf\",\"data\":{\"status\":\"EstCredMqtt1\"}}";
        log.info("Checking broker for printer {}", printerId);
        
        try {
            String responseJson = legacyClient.publishAndAwait(ctx.compactMac(), payload, "StaInf", 20);
            return parseBrokerResponse(responseJson);
        } catch (Exception e) {
            log.warn("Printer didn't respond on legacy broker, trying new broker...", e);
            
            CompletableFuture<FiscalMqttResponseItem> future = syncResponseAwaiter.registerWithMatcher(ctx.compactMac(), item -> {
                return item.cmd() != null && item.cmd().trim().equals("StaInf");
            });
            try {
                mqttService.publish(ctx.topic(), payload);
                FiscalMqttResponseItem item = future.get(20, TimeUnit.SECONDS);
                
                if (item.dataS() != null) {
                    JsonNode dataSNode = null;
                    try {
                        dataSNode = objectMapper.readTree(item.dataS());
                    } catch (Exception ex) {
                        log.warn("Failed to parse dataS as JSON string, using as text");
                    }
                    String host = "";
                    if (dataSNode != null && dataSNode.has("host")) {
                        host = dataSNode.get("host").asText();
                    }
                    boolean isOldBroker = host.contains("13.51");
                    return new BrokerMigrationBrokerResult(host, isOldBroker);
                }
                throw new EnajenacionProtocolException("Missing dataS in response from new broker.");
            } catch (Exception ex) {
                log.error("Printer didn't respond on new broker either", ex);
                throw new EnajenacionProtocolException("La impresora no respondió en ningún broker");
            } finally {
                syncResponseAwaiter.cancel(ctx.compactMac());
            }
        }
    }

    private BrokerMigrationBrokerResult parseBrokerResponse(String responseJson) throws Exception {
        JsonNode node = objectMapper.readTree(responseJson);
        if (node.has("dataS")) {
            JsonNode dataSNode = node.get("dataS");
            if (dataSNode.isTextual()) {
                try {
                    dataSNode = objectMapper.readTree(dataSNode.asText());
                } catch (Exception e) {
                    log.warn("dataS is not a valid JSON string");
                }
            }
            String host = dataSNode.has("host") ? dataSNode.get("host").asText() : "";
            boolean isOldBroker = host.contains("13.51");
            return new BrokerMigrationBrokerResult(host, isOldBroker);
        }
        throw new EnajenacionProtocolException("Missing dataS in legacy response.");
    }

    public BrokerMigrationBrokerConfigResult configureBroker(Long printerId) {
        PrinterContext ctx = resolvePrinter(printerId);
        String payload = "{\"cmd\":\"configMqttAEG\",\"data\":{\"addrBroker\":\"206.189.231.128\",\"portBroker\":1883,\"userBroker\":\"aegptrfiscal2024\",\"passwBroker\":\"aegseniat2024\",\"topicRx\":\"Comando\",\"topicTx\":\"Respuesta\",\"topicDoc\":\"Documento\"}}";
        log.info("Configuring new broker for printer {}", printerId);
        
        try {
            String responseJson = legacyClient.publishAndAwait(ctx.compactMac(), payload, "configMqttAEG", 20);
            JsonNode node = objectMapper.readTree(responseJson);
            if (node.has("code") && node.get("code").asInt() == 0) {
                return new BrokerMigrationBrokerConfigResult(BrokerMigrationBrokerConfigResult.Outcome.MIGRATED, "Broker migrado exitosamente (ACK recibido).");
            } else {
                return new BrokerMigrationBrokerConfigResult(BrokerMigrationBrokerConfigResult.Outcome.REJECTED, "La impresora rechazó la configuración.");
            }
        } catch (Exception e) {
            log.warn("Timeout or error waiting for configureBroker ACK, waiting to verify on new broker...", e);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            String verifyPayload = "{\"cmd\":\"StaInf\",\"data\":{\"status\":\"EstCredMqtt1\"}}";
            CompletableFuture<FiscalMqttResponseItem> future = syncResponseAwaiter.registerWithMatcher(ctx.compactMac(), item -> {
                return item.cmd() != null && item.cmd().trim().equals("StaInf");
            });
            try {
                mqttService.publish(ctx.topic(), verifyPayload);
                FiscalMqttResponseItem item = future.get(10, TimeUnit.SECONDS);
                
                if (item.dataS() != null) {
                    return new BrokerMigrationBrokerConfigResult(BrokerMigrationBrokerConfigResult.Outcome.MIGRATED, "ACK perdido, migración confirmada en broker nuevo");
                }
            } catch (Exception ex) {
                log.warn("Failed to confirm on new broker", ex);
            } finally {
                syncResponseAwaiter.cancel(ctx.compactMac());
            }
            return new BrokerMigrationBrokerConfigResult(BrokerMigrationBrokerConfigResult.Outcome.UNCERTAIN, "No se pudo confirmar. Verifique la impresora e intente verificar el broker nuevamente.");
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
