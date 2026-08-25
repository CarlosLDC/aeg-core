package com.aeg.core.fiscalizacion.mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class FiscalizacionPayloadBuilder {

    private final ObjectMapper objectMapper;
    private volatile String configSpiffsTemplate;

    public FiscalizacionPayloadBuilder(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildAckSuccess() {
        return buildAck(0, FiscalizacionConstants.MSG_LISTA, true);
    }

    public String buildAckError(String msj) {
        return buildAck(1, msj == null ? "Error de validación" : msj, false);
    }

    public String buildConfigSpiffsPayload() {
        return loadConfigSpiffsTemplate();
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
                throw new FiscalizacionProtocolException("Failed to load configSPIFFS template: " + ex.getMessage());
            }
        }
    }

    private String buildAck(int code, String msj, boolean includeAccess) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("cmd", FiscalizacionConstants.CMD_RX_PTR_FISCALIZAR_REMOTO);
            root.put("code", code);
            ObjectNode data = root.putObject("data");
            data.put("msj", msj);
            if (includeAccess) {
                data.put("Access", "config");
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new FiscalizacionProtocolException("Failed to build RxPtrFiscalizarRemoto payload");
        }
    }
}
