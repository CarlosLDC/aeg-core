package com.aeg.core.enajenacion.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class EnajenacionMqttResponses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EnajenacionMqttResponses() {
    }

    static String ptrEnajenar(String fiscalSerial, String macWithColons) {
        return """
                {"cmd":"ptrEnajenar","data":{"ptrReg":"%s","macAddr":"%s"}}
                """.formatted(fiscalSerial, macWithColons).trim();
    }

    static String dnfSuccess() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        items.add(item("aperDNF", 0));
        items.add(item("efeNeDAnJuCeDNF", 0));
        IntStream.range(0, 8).forEach(i -> items.add(item("efeNoDAnJuCeDNF", 0)));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_DNF, 0, EnajenacionConstants.DNF_END_OK));
        return writeJson(items);
    }

    static String fiscalRifSuccess() {
        return writeJson(new FiscalMqttResponseItem(EnajenacionConstants.CMD_FISCAL_AEG, 0, 0));
    }

    static String wFileSpiffSuccess() {
        return writeJson(new FiscalMqttResponseItem(EnajenacionConstants.CMD_W_FILE_SPIFF, 0, 0));
    }

    static String staInfSuccess(String fiscalSerial) {
        return writeJson(new FiscalMqttResponseItem(EnajenacionConstants.CMD_STA_INF, 0, null, fiscalSerial));
    }

    static String invoiceSuccess() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        IntStream.range(0, 5).forEach(i -> items.add(item("proF", 0)));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_SUB_TO_F, 0, EnajenacionConstants.SUBTOTAL_DATA_D));
        items.add(item("fpaF", 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_FAC, 0, EnajenacionConstants.INVOICE_END_OK));
        return writeJson(items);
    }

    static String creditNoteSuccess() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        items.add(item("nroFacNC", 0));
        items.add(item("fechFacNC", 0));
        items.add(item("conSerNC", 0));
        items.add(item("rifCiNC", 0));
        items.add(item("razSocNC", 0));
        IntStream.range(0, 5).forEach(i -> items.add(new FiscalMqttResponseItem(
                EnajenacionConstants.CMD_PROD_NC, 0, EnajenacionConstants.PROD_NC_LINE_DATA_D)));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_PO_NC, 0, EnajenacionConstants.SUBTOTAL_DATA_D));
        items.add(item("fpaNC", 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_NC, 0, EnajenacionConstants.CREDIT_NOTE_END_OK));
        return writeJson(items);
    }

    static String reportZSuccess() {
        return writeJson(new FiscalMqttResponseItem(EnajenacionConstants.CMD_GEN_IMP_REP_Z, 0, 0));
    }

    static String dnfFailure() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        IntStream.range(0, 10).forEach(i -> items.add(item("efeNoDAnJuCeDNF", 0)));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_DNF, 0, 0));
        return writeJson(items);
    }

    private static FiscalMqttResponseItem item(String cmd, int dataD) {
        return new FiscalMqttResponseItem(cmd, 0, dataD);
    }

    private static String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
