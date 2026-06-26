package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

class FiscalResponseValidatorTest {

    private final FiscalResponseValidator validator = new FiscalResponseValidator();

    @Test
    void acceptsValidDnfResponse() {
        List<FiscalMqttResponseItem> items = validDnfResponse();

        validator.validateDnfResponse(items);
    }

    @Test
    void acceptsDnfWithArbitraryDataD() {
        List<FiscalMqttResponseItem> items = validDnfResponse();
        items.set(10, new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_DNF, 0, 42));

        validator.validateDnfResponse(items);
    }

    @Test
    void rejectsDnfWithWrongCommandOrder() {
        List<FiscalMqttResponseItem> items = validDnfResponse();
        items.set(1, new FiscalMqttResponseItem("efeNoDAnJuCeDNF", 0, 0));

        assertThatThrownBy(() -> validator.validateDnfResponse(items))
                .isInstanceOf(EnajenacionProtocolException.class);
    }

    @Test
    void acceptsValidStaInfResponse() {
        validator.validateStaInfResponse(
                new FiscalMqttResponseItem(EnajenacionConstants.CMD_STA_INF, 0, null, "GRA0000017"),
                "GRA0000017");
    }

    @Test
    void acceptsStaInfWithTrimmedCmd() {
        validator.validateStaInfResponse(
                new FiscalMqttResponseItem(" StaInf ", 0, null, "GRA0000017"),
                "GRA0000017");
    }

    @Test
    void rejectsStaInfWithMismatchedDataS() {
        assertThatThrownBy(() -> validator.validateStaInfResponse(
                        new FiscalMqttResponseItem(EnajenacionConstants.CMD_STA_INF, 0, null, "GRA0000099"),
                        "GRA0000017"))
                .isInstanceOf(EnajenacionProtocolException.class);
    }

    @Test
    void validateStaInfRegistrationResponseReturnsDataSWithoutSerialMatch() {
        org.assertj.core.api.Assertions.assertThat(
                validator.validateStaInfRegistrationResponse(
                        new FiscalMqttResponseItem(EnajenacionConstants.CMD_STA_INF, 0, 0, "GRA0000017")))
                .isEqualTo("GRA0000017");
    }

    @Test
    void acceptsValidInvoiceResponse() {
        validator.validateInvoiceResponse(validInvoiceResponse());
    }

    @Test
    void rejectsInvoiceWithWrongCommandOrder() {
        List<FiscalMqttResponseItem> items = validInvoiceResponse();
        items.set(5, new FiscalMqttResponseItem("fpaF", 0, 0));

        assertThatThrownBy(() -> validator.validateInvoiceResponse(items))
                .isInstanceOf(EnajenacionProtocolException.class);
    }

    @Test
    void acceptsInvoiceWithArbitraryDataD() {
        List<FiscalMqttResponseItem> items = validInvoiceResponse();
        items.set(5, new FiscalMqttResponseItem(EnajenacionConstants.CMD_SUB_TO_F, 0, 123));
        items.set(7, new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_FAC, 0, 999));

        validator.validateInvoiceResponse(items);
    }

    @Test
    void validateAnnualInspectionTestInvoiceResponseReturnsEndFacDataD() {
        int numero = validator.validateAnnualInspectionTestInvoiceResponse(validAnnualInspectionInvoiceResponse());

        org.assertj.core.api.Assertions.assertThat(numero).isEqualTo(7);
    }

    @Test
    void rejectsAnnualInspectionInvoiceWithNonZeroCode() {
        List<FiscalMqttResponseItem> items = validAnnualInspectionInvoiceResponse();
        items.set(3, new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_FAC, 1, 7));

        assertThatThrownBy(() -> validator.validateAnnualInspectionTestInvoiceResponse(items))
                .isInstanceOf(EnajenacionProtocolException.class);
    }

    @Test
    void validateAnnualInspectionTestCreditNoteResponseAcceptsValidArray() {
        validator.validateAnnualInspectionTestCreditNoteResponse(validAnnualInspectionCreditNoteResponse());
    }

    @Test
    void rejectsAnnualInspectionCreditNoteWithNonZeroEndNcCode() {
        List<FiscalMqttResponseItem> items = validAnnualInspectionCreditNoteResponse();
        items.set(8, new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_NC, 1, 10));

        assertThatThrownBy(() -> validator.validateAnnualInspectionTestCreditNoteResponse(items))
                .isInstanceOf(EnajenacionProtocolException.class);
    }

    @Test
    void acceptsValidCreditNoteResponse() {
        validator.validateCreditNoteResponse(validCreditNoteResponse());
    }

    @Test
    void rejectsCreditNoteWithWrongCommandOrder() {
        List<FiscalMqttResponseItem> items = validCreditNoteResponse();
        items.set(10, new FiscalMqttResponseItem("fpaNC", 0, 0));

        assertThatThrownBy(() -> validator.validateCreditNoteResponse(items))
                .isInstanceOf(EnajenacionProtocolException.class);
    }

    @Test
    void acceptsCreditNoteWithArbitraryDataD() {
        List<FiscalMqttResponseItem> items = validCreditNoteResponse();
        items.set(5, new FiscalMqttResponseItem(EnajenacionConstants.CMD_PROD_NC, 0, 1));
        items.set(12, new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_NC, 0, 77));

        validator.validateCreditNoteResponse(items);
    }

    @Test
    void acceptsValidReportZResponse() {
        validator.validateReportZResponse(
                new FiscalMqttResponseItem(EnajenacionConstants.CMD_GEN_IMP_REP_Z, 0, 0));
    }

    @Test
    void acceptsValidSetDateRevOResponse() {
        validator.validateSetDateRevOResponse(
                new FiscalMqttResponseItem(EnajenacionConstants.CMD_SET_DATE_REV_O, 0, 0));
    }

    @Test
    void acceptsReportZWithArbitraryDataD() {
        validator.validateReportZResponse(
                new FiscalMqttResponseItem(EnajenacionConstants.CMD_GEN_IMP_REP_Z, 0, 99));
    }

    private static List<FiscalMqttResponseItem> validDnfResponse() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        items.add(new FiscalMqttResponseItem("aperDNF", 0, 0));
        items.add(new FiscalMqttResponseItem("efeNeDAnJuCeDNF", 0, 0));
        for (int i = 0; i < 8; i++) {
            items.add(new FiscalMqttResponseItem("efeNoDAnJuCeDNF", 0, 0));
        }
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_DNF, 0, EnajenacionConstants.DNF_END_OK));
        return items;
    }

    private static List<FiscalMqttResponseItem> validInvoiceResponse() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            items.add(new FiscalMqttResponseItem("proF", 0, 0));
        }
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_SUB_TO_F, 0, EnajenacionConstants.SUBTOTAL_DATA_D));
        items.add(new FiscalMqttResponseItem("fpaF", 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_FAC, 0, EnajenacionConstants.INVOICE_END_OK));
        return items;
    }

    private static List<FiscalMqttResponseItem> validAnnualInspectionInvoiceResponse() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        items.add(new FiscalMqttResponseItem("proF", 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_SUB_TO_F, 0, 100));
        items.add(new FiscalMqttResponseItem("fpaF", 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_FAC, 0, 7));
        return items;
    }

    private static List<FiscalMqttResponseItem> validAnnualInspectionCreditNoteResponse() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        items.add(new FiscalMqttResponseItem("nroFacNC", 0, 0));
        items.add(new FiscalMqttResponseItem("fechFacNC", 0, 0));
        items.add(new FiscalMqttResponseItem("conSerNC", 0, 0));
        items.add(new FiscalMqttResponseItem("rifCiNC", 0, 0));
        items.add(new FiscalMqttResponseItem("razSocNC", 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_PROD_NC, 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_PO_NC, 0, 0));
        items.add(new FiscalMqttResponseItem("fpaNC", 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_NC, 0, 10));
        return items;
    }

    private static List<FiscalMqttResponseItem> validCreditNoteResponse() {
        List<FiscalMqttResponseItem> items = new ArrayList<>();
        items.add(new FiscalMqttResponseItem("nroFacNC", 0, 0));
        items.add(new FiscalMqttResponseItem("fechFacNC", 0, 0));
        items.add(new FiscalMqttResponseItem("conSerNC", 0, 0));
        items.add(new FiscalMqttResponseItem("rifCiNC", 0, 0));
        items.add(new FiscalMqttResponseItem("razSocNC", 0, 0));
        for (int i = 0; i < 5; i++) {
            items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_PROD_NC, 0, EnajenacionConstants.PROD_NC_LINE_DATA_D));
        }
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_PO_NC, 0, EnajenacionConstants.SUBTOTAL_DATA_D));
        items.add(new FiscalMqttResponseItem("fpaNC", 0, 0));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_NC, 0, EnajenacionConstants.CREDIT_NOTE_END_OK));
        return items;
    }
}
