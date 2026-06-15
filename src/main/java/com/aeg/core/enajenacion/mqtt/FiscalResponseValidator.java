package com.aeg.core.enajenacion.mqtt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

@Component
public class FiscalResponseValidator {

    public void validateDnfResponse(List<FiscalMqttResponseItem> items) {
        if (items == null || items.size() != 11) {
            throw new EnajenacionProtocolException("DNF response must contain 11 items");
        }
        for (FiscalMqttResponseItem item : items) {
            assertSuccessCode(item);
            if (EnajenacionConstants.CMD_END_DNF.equals(item.cmd())) {
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
        if (items == null || items.size() != 8) {
            throw new EnajenacionProtocolException("Invoice response must contain 8 items");
        }
        for (FiscalMqttResponseItem item : items) {
            assertSuccessCode(item);
            if (EnajenacionConstants.CMD_SUB_TO_F.equals(item.cmd())) {
                assertDataD(item, EnajenacionConstants.SUBTOTAL_DATA_D);
            } else if (EnajenacionConstants.CMD_END_FAC.equals(item.cmd())) {
                assertDataD(item, EnajenacionConstants.INVOICE_END_OK);
            } else {
                assertDataD(item, 0);
            }
        }
    }

    public void validateCreditNoteResponse(List<FiscalMqttResponseItem> items) {
        if (items == null || items.size() != 13) {
            throw new EnajenacionProtocolException("Credit note response must contain 13 items");
        }
        for (FiscalMqttResponseItem item : items) {
            assertSuccessCode(item);
            if (EnajenacionConstants.CMD_PROD_NC.equals(item.cmd())) {
                assertDataD(item, EnajenacionConstants.PROD_NC_LINE_DATA_D);
            } else if (EnajenacionConstants.CMD_END_PO_NC.equals(item.cmd())) {
                assertDataD(item, EnajenacionConstants.SUBTOTAL_DATA_D);
            } else if (EnajenacionConstants.CMD_END_NC.equals(item.cmd())) {
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
