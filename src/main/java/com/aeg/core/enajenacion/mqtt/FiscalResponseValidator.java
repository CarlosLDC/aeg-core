package com.aeg.core.enajenacion.mqtt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

@Component
public class FiscalResponseValidator {

    private static final List<String> DNF_RESPONSE_COMMANDS = List.of(
            "aperDNF",
            "efeNeDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            "efeNoDAnJuCeDNF",
            EnajenacionConstants.CMD_END_DNF);

    private static final List<String> INVOICE_RESPONSE_COMMANDS = List.of(
            "proF",
            "proF",
            "proF",
            "proF",
            "proF",
            EnajenacionConstants.CMD_SUB_TO_F,
            "fpaF",
            EnajenacionConstants.CMD_END_FAC);

    private static final List<String> CREDIT_NOTE_RESPONSE_COMMANDS = List.of(
            "nroFacNC",
            "fechFacNC",
            "conSerNC",
            "rifCiNC",
            "razSocNC",
            EnajenacionConstants.CMD_PROD_NC,
            EnajenacionConstants.CMD_PROD_NC,
            EnajenacionConstants.CMD_PROD_NC,
            EnajenacionConstants.CMD_PROD_NC,
            EnajenacionConstants.CMD_PROD_NC,
            EnajenacionConstants.CMD_END_PO_NC,
            "fpaNC",
            EnajenacionConstants.CMD_END_NC);

    public void validateDnfResponse(List<FiscalMqttResponseItem> items) {
        if (items == null || items.size() != DNF_RESPONSE_COMMANDS.size()) {
            throw new EnajenacionProtocolException(
                    "DNF response must contain " + DNF_RESPONSE_COMMANDS.size() + " items");
        }
        for (int i = 0; i < items.size(); i++) {
            FiscalMqttResponseItem item = items.get(i);
            String expectedCmd = DNF_RESPONSE_COMMANDS.get(i);
            if (item == null) {
                throw new EnajenacionProtocolException("DNF response item at index " + i + " is null");
            }
            if (!cmdEquals(item.cmd(), expectedCmd)) {
                throw new EnajenacionProtocolException(
                        "Unexpected DNF response cmd at index " + i + ", expected " + expectedCmd);
            }
            assertSuccessCode(item);
            if (cmdEquals(item.cmd(), EnajenacionConstants.CMD_END_DNF)) {
                assertDataD(item, EnajenacionConstants.DNF_END_OK);
            } else {
                assertDataD(item, 0);
            }
        }
    }

    public void validateObjectResponse(FiscalMqttResponseItem item, String expectedCmd) {
        if (item == null || !cmdEquals(item.cmd(), expectedCmd)) {
            throw new EnajenacionProtocolException("Unexpected response cmd, expected " + expectedCmd);
        }
        assertSuccessCode(item);
        assertDataD(item, 0);
    }

    public void validateStaInfResponse(FiscalMqttResponseItem item, String expectedFiscalSerial) {
        if (item == null || !cmdEquals(item.cmd(), EnajenacionConstants.CMD_STA_INF)) {
            throw new EnajenacionProtocolException("Unexpected response cmd, expected StaInf");
        }
        assertSuccessCode(item);
        if (item.dataS() == null || item.dataS().isBlank()) {
            throw new EnajenacionProtocolException("StaInf response missing dataS");
        }
        if (!item.dataS().trim().equalsIgnoreCase(expectedFiscalSerial.trim())) {
            throw new EnajenacionProtocolException(
                    "StaInf dataS mismatch: expected "
                            + expectedFiscalSerial
                            + " but was "
                            + item.dataS());
        }
    }

    public void validateInvoiceResponse(List<FiscalMqttResponseItem> items) {
        if (items == null || items.size() != INVOICE_RESPONSE_COMMANDS.size()) {
            throw new EnajenacionProtocolException(
                    "Invoice response must contain " + INVOICE_RESPONSE_COMMANDS.size() + " items");
        }
        for (int i = 0; i < items.size(); i++) {
            FiscalMqttResponseItem item = items.get(i);
            String expectedCmd = INVOICE_RESPONSE_COMMANDS.get(i);
            if (item == null) {
                throw new EnajenacionProtocolException("Invoice response item at index " + i + " is null");
            }
            if (!cmdEquals(item.cmd(), expectedCmd)) {
                throw new EnajenacionProtocolException(
                        "Unexpected invoice response cmd at index " + i + ", expected " + expectedCmd);
            }
            assertSuccessCode(item);
            if (cmdEquals(item.cmd(), EnajenacionConstants.CMD_SUB_TO_F)) {
                assertDataD(item, EnajenacionConstants.SUBTOTAL_DATA_D);
            } else if (cmdEquals(item.cmd(), EnajenacionConstants.CMD_END_FAC)) {
                assertDataD(item, EnajenacionConstants.INVOICE_END_OK);
            } else {
                assertDataD(item, 0);
            }
        }
    }

    public void validateCreditNoteResponse(List<FiscalMqttResponseItem> items) {
        if (items == null || items.size() != CREDIT_NOTE_RESPONSE_COMMANDS.size()) {
            throw new EnajenacionProtocolException(
                    "Credit note response must contain " + CREDIT_NOTE_RESPONSE_COMMANDS.size() + " items");
        }
        for (int i = 0; i < items.size(); i++) {
            FiscalMqttResponseItem item = items.get(i);
            String expectedCmd = CREDIT_NOTE_RESPONSE_COMMANDS.get(i);
            if (item == null) {
                throw new EnajenacionProtocolException("Credit note response item at index " + i + " is null");
            }
            if (!cmdEquals(item.cmd(), expectedCmd)) {
                throw new EnajenacionProtocolException(
                        "Unexpected credit note response cmd at index " + i + ", expected " + expectedCmd);
            }
            assertSuccessCode(item);
            if (cmdEquals(item.cmd(), EnajenacionConstants.CMD_PROD_NC)) {
                assertDataD(item, EnajenacionConstants.PROD_NC_LINE_DATA_D);
            } else if (cmdEquals(item.cmd(), EnajenacionConstants.CMD_END_PO_NC)) {
                assertDataD(item, EnajenacionConstants.SUBTOTAL_DATA_D);
            } else if (cmdEquals(item.cmd(), EnajenacionConstants.CMD_END_NC)) {
                assertDataD(item, EnajenacionConstants.CREDIT_NOTE_END_OK);
            } else {
                assertDataD(item, 0);
            }
        }
    }

    public void validateReportZResponse(FiscalMqttResponseItem item) {
        validateObjectResponse(item, EnajenacionConstants.CMD_GEN_IMP_REP_Z);
    }

    private static void assertSuccessCode(FiscalMqttResponseItem item) {
        if (item.code() == null || item.code() != 0) {
            throw new EnajenacionProtocolException("Command " + item.cmd() + " failed with code " + item.code());
        }
    }

    private static void assertDataD(FiscalMqttResponseItem item, int expected) {
        if (item.dataD() == null || item.dataD() != expected) {
            throw new EnajenacionProtocolException(
                    "Command " + item.cmd() + " expected dataD=" + expected + " but was " + item.dataD());
        }
    }

    private static boolean cmdEquals(String actual, String expected) {
        return normalizeCmd(actual) != null
                && normalizeCmd(actual).equalsIgnoreCase(normalizeCmd(expected));
    }

    static String normalizeCmd(String cmd) {
        return cmd == null ? null : cmd.trim();
    }
}
