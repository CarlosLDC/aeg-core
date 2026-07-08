package com.aeg.core.tools.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ToolsTestDocumentsPayloadBuilderTest {

    private ToolsTestDocumentsPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ToolsTestDocumentsPayloadBuilder(new ObjectMapper());
    }

    @Test
    void testInvoicePayloadsMatchElectronSequence() {
        var payloads = builder.testInvoicePayloads();

        assertEquals(8, payloads.size());
        assertTrue(payloads.get(0).contains("\"cmd\":\"proF\""));
        assertTrue(payloads.get(7).contains("\"cmd\":\"endFac\""));
    }

    @Test
    void testCreditNotePayloadsIncludePrinterSerial() {
        var payloads = builder.testCreditNotePayloads("GRA12345");

        assertEquals(13, payloads.size());
        assertTrue(payloads.stream().anyMatch(p -> p.contains("\"cmd\":\"conSerNC\"") && p.contains("GRA12345")));
        assertTrue(payloads.get(payloads.size() - 1).contains("\"cmd\":\"endNC\""));
    }

    @Test
    void testDebitNotePayloadsIncludePrinterSerial() {
        var payloads = builder.testDebitNotePayloads("GRA12345");

        assertEquals(13, payloads.size());
        assertTrue(payloads.stream().anyMatch(p -> p.contains("\"cmd\":\"conSerND\"") && p.contains("GRA12345")));
        assertTrue(payloads.get(payloads.size() - 1).contains("\"cmd\":\"endND\""));
    }

    @Test
    void testGenerateZPayloadUsesGenImpRepZ() {
        var payloads = builder.testGenerateZPayloads();

        assertEquals(1, payloads.size());
        assertTrue(payloads.get(0).contains("\"cmd\":\"genImpRepZ\""));
    }
}
