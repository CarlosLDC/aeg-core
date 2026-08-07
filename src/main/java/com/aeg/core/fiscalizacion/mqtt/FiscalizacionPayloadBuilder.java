package com.aeg.core.fiscalizacion.mqtt;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class FiscalizacionPayloadBuilder {

    private final ObjectMapper objectMapper;

    public FiscalizacionPayloadBuilder(@Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildAckSuccess() {
        return buildAck(0, FiscalizacionConstants.MSG_LISTA, true);
    }

    public String buildAckError(String msj) {
        return buildAck(1, msj == null ? "Error de validación" : msj, false);
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
