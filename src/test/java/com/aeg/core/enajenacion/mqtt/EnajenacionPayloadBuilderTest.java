package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class EnajenacionPayloadBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EnajenacionPayloadBuilder builder = new EnajenacionPayloadBuilder(objectMapper, "");

    @Test
    void fiscalRifPayloadUsesClientRifAndBusinessName() throws Exception {
        EnajenacionContext context = context();

        JsonNode root = objectMapper.readTree(builder.buildFiscalRifPayload(context));

        assertThat(root.path("cmd").asText()).isEqualTo("fiscalAEG");
        JsonNode data = root.path("data");
        assertThat(data.path("nameFile").asText()).isEqualTo("rifEmp.json");
        assertThat(data.path("Access").asText()).isEqualTo("config");
        assertThat(data.path("contenido").path("tituloSeniat").asText()).isEqualTo("SENIAT");
        assertThat(data.path("contenido").path("rifEmp").asText()).isEqualTo("J-500662998");
        assertThat(data.path("contenido").path("nomEmp").asText())
                .isEqualTo("INVERSIONES SHOP COMPUTER 2020, C.A.");
    }

    @Test
    void headerPayloadUsesClientAddressLines() throws Exception {
        EnajenacionContext context = context();

        JsonNode root = objectMapper.readTree(builder.buildHeaderPayload(context));

        assertThat(root.path("cmd").asText()).isEqualTo("wFileSPIFF");
        JsonNode data = root.path("data");
        assertThat(data.path("Access").asText()).isEqualTo("AeG-1968-2024");
        assertThat(data.path("nameFile").asText()).isEqualTo("paramFacSPIFF.json");
        assertThat(textValues(data.path("contenido").path("encFacFijo")))
                .containsExactly(
                        "AV. URDANETA EDIF. CASA BERA",
                        "PISO PB LOCAL -005-C URB. LA CANDELARIA",
                        "CARACAS, DISTRITO CAPITAL",
                        "CONTRIBUYENTE ORDINARIO");
        assertThat(data.path("contenido").has("pieFacFijo")).isFalse();
    }

    @Test
    void headerPayloadUsesStoredPrinterTicketLines() throws Exception {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J503752890",
                "ABASTO HERMANOS YEISAR 2023, C.A.",
                "CONTRIBUYENTE ORDINARIO",
                "ignored",
                "",
                "ignored",
                java.util.List.of(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of("PIE DE TICKET"));

        JsonNode root = objectMapper.readTree(builder.buildHeaderPayload(context));
        JsonNode contenido = root.path("data").path("contenido");

        assertThat(textValues(contenido.path("encFacFijo")))
                .containsExactly(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "CONTRIBUYENTE ORDINARIO");
        assertThat(textValues(contenido.path("pieFacFijo"))).containsExactly("PIE DE TICKET");
    }

    @Test
    void headerPayloadPreservesSpanishAccentsInTicketLines() throws Exception {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J503752890",
                "ABASTO HERMANOS YEISAR 2023, C.A.",
                "CONTRIBUYENTE ORDINARIO",
                "ignored",
                "",
                "ignored",
                java.util.List.of(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "Línea de información"),
                java.util.List.of("Gracias por su compra — vuelva pronto"));

        JsonNode root = objectMapper.readTree(builder.buildHeaderPayload(context));
        JsonNode contenido = root.path("data").path("contenido");

        assertThat(textValues(contenido.path("encFacFijo")))
                .containsExactly(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "Línea de información");
        assertThat(textValues(contenido.path("pieFacFijo")))
                .containsExactly("Gracias por su compra - vuelva pronto");
    }

    @Test
    void headerPayloadOmitsBlankSecondAddressLine() throws Exception {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J503752890",
                "ABASTO HERMANOS YEISAR 2023, C.A.",
                "CONTRIBUYENTE ORDINARIO",
                "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                "",
                "PUERTO LA CRUZ, ANZOATEGUI",
                java.util.List.of(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "CONTRIBUYENTE ORDINARIO"),
                java.util.List.of());

        JsonNode root = objectMapper.readTree(builder.buildHeaderPayload(context));

        assertThat(textValues(root.path("data").path("contenido").path("encFacFijo")))
                .containsExactly(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "CONTRIBUYENTE ORDINARIO");
    }

    @Test
    void headerPayloadIncludesConfiguredTicketFooterLines() throws Exception {
        EnajenacionPayloadBuilder builderWithFooter = new EnajenacionPayloadBuilder(
                objectMapper,
                "PIE DE TICKET 01|PIE DE TICKET 02|PIE DE TICKET 03");

        JsonNode root = objectMapper.readTree(builderWithFooter.buildHeaderPayload(context()));

        JsonNode data = root.path("data");
        assertThat(data.path("Access").asText()).isEqualTo("AeG-1968-2024");
        assertThat(textValues(data.path("contenido").path("pieFacFijo")))
                .containsExactly("PIE DE TICKET 01", "PIE DE TICKET 02", "PIE DE TICKET 03");
    }

    @Test
    void configSpiffsPayloadMatchesTaxAndPaymentTemplate() throws Exception {
        JsonNode root = objectMapper.readTree(builder.buildConfigSpiffsPayload());

        assertThat(root.path("cmd").asText()).isEqualTo("wFileSPIFF");
        JsonNode data = root.path("data");
        assertThat(data.path("nameFile").asText()).isEqualTo("configSPIFFS.json");
        assertThat(data.has("Access")).isFalse();

        JsonNode content = data.path("contenido");
        assertThat(content.path("simMonL").asText()).isEqualTo("Bs");
        assertThat(intValues(content.path("impArt").path("valor")))
                .containsExactly(0, 1600, 800, 3100, 0);
        assertThat(content.path("formPago").path("tituloFormPag").asText()).isEqualTo("FORMA DE PAGO");
        assertThat(textValues(content.path("formPago").path("desc")))
                .containsExactlyElementsOf(List.of(
                        "EFECTIVO",
                        "T. DEBITO",
                        "T. CREDITO",
                        "TRANSFERENCIA",
                        "PAGO MOVIL",
                        "BIOPAGO",
                        "EFECTIVO 7",
                        "EFECTIVO 8",
                        "EFECTIVO 9",
                        "EFECTIVO 10",
                        "DIVISA 1",
                        "DIVISA 2",
                        "DIVISA 3",
                        "DIVISA 4",
                        "DIVISA 5",
                        "DIVISA 6"));
        assertThat(intValues(content.path("formPago").path("impG")))
                .containsExactly(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 300, 300, 300, 300, 300, 300);
    }

    @Test
    void invoicePayloadMapsMultiLineDescriptionAcrossProfCommands() throws Exception {
        String line1 = "L".repeat(70);
        String line2 = "M".repeat(70);
        JsonNode root = objectMapper.readTree(builder.buildInvoicePayload(line1 + "\n" + line2));

        assertThat(root.get(0).path("data").path("des01").asText()).hasSize(60).startsWith("L");
        assertThat(root.get(1).path("data").path("des01").asText()).hasSize(60).startsWith("M");
        assertThat(root.get(2).path("data").path("des01").asText()).isEmpty();
        assertThat(root.get(3).path("data").path("des01").asText()).isEmpty();
        assertThat(root.get(4).path("data").path("des01").asText()).isEmpty();
    }

    @Test
    void invoicePayloadPreservesSpanishAccentsInProductDescription() throws Exception {
        JsonNode root = objectMapper.readTree(
                builder.buildInvoicePayload("Producto de prueba — información técnica"));

        for (int i = 0; i < 5; i++) {
            assertThat(root.get(i).path("data").path("des01").asText())
                    .isEqualTo("Producto de prueba - información técnic");
        }
    }

    @Test
    void invoicePayloadMatchesFiscalTestInvoiceCommand() throws Exception {
        JsonNode root = objectMapper.readTree(builder.buildInvoicePayload());

        assertThat(root).hasSize(8);
        for (int i = 0; i < 5; i++) {
            JsonNode item = root.get(i);
            assertThat(item.path("cmd").asText()).isEqualTo("proF");
            JsonNode data = item.path("data");
            assertThat(data.path("pre").asInt()).isEqualTo(100);
            assertThat(data.path("cant").asInt()).isEqualTo(1000);
            assertThat(data.path("imp").asInt()).isEqualTo(i + 1);
            assertThat(data.path("des01").asText()).isEqualTo("PRODUCTO");
        }

        assertThat(root.get(5).path("cmd").asText()).isEqualTo("subToF");
        assertThat(root.get(5).path("data").asInt()).isEqualTo(1);
        assertThat(root.get(5).path("valor").asInt()).isEqualTo(0);
        assertThat(root.get(6).path("cmd").asText()).isEqualTo("fpaF");
        assertThat(root.get(6).path("data").path("tipo").asInt()).isEqualTo(1);
        assertThat(root.get(6).path("data").path("monto").asInt()).isEqualTo(-1);
        assertThat(root.get(6).path("data").path("tasaConv").asInt()).isEqualTo(0);
        assertThat(root.get(7).path("cmd").asText()).isEqualTo("endFac");
        assertThat(root.get(7).path("data").asInt()).isEqualTo(1);
    }

    @Test
    void annualInspectionTestInvoicePayloadMatchesSingleLineCommand() throws Exception {
        JsonNode root = objectMapper.readTree(builder.buildAnnualInspectionTestInvoicePayload("COLGATE TOTAL"));

        assertThat(root).hasSize(4);
        JsonNode prof = root.get(0);
        assertThat(prof.path("cmd").asText()).isEqualTo("proF");
        assertThat(prof.path("data").path("pre").asInt()).isEqualTo(100);
        assertThat(prof.path("data").path("cant").asInt()).isEqualTo(1000);
        assertThat(prof.path("data").path("imp").asInt()).isEqualTo(1);
        assertThat(prof.path("data").path("des01").asText()).isEqualTo("COLGATE TOTAL");
        assertThat(root.get(1).path("cmd").asText()).isEqualTo("subToF");
        assertThat(root.get(2).path("cmd").asText()).isEqualTo("fpaF");
        assertThat(root.get(3).path("cmd").asText()).isEqualTo("endFac");
    }

    @Test
    void annualInspectionTestCreditNotePayloadMatchesSingleLineCommand() throws Exception {
        JsonNode root = objectMapper.readTree(builder.buildAnnualInspectionTestCreditNotePayload(
                7,
                "GRA0000017",
                LocalDate.of(2026, 6, 26),
                "COLGATE TOTAL"));

        assertThat(root).hasSize(9);
        assertThat(root.get(0).path("cmd").asText()).isEqualTo("nroFacNC");
        assertThat(root.get(0).path("data").asInt()).isEqualTo(7);
        assertThat(root.get(1).path("cmd").asText()).isEqualTo("fechFacNC");
        assertThat(root.get(1).path("data").asText()).isEqualTo("26/06/2026");
        assertThat(root.get(2).path("cmd").asText()).isEqualTo("conSerNC");
        assertThat(root.get(2).path("data").asText()).isEqualTo("GRA0000017");
        assertThat(root.get(3).path("cmd").asText()).isEqualTo("rifCiNC");
        assertThat(root.get(3).path("data").asText()).isEqualTo("V00000000");
        assertThat(root.get(4).path("cmd").asText()).isEqualTo("razSocNC");
        assertThat(root.get(4).path("data").path("razSoc").get(0).asText())
                .isEqualTo("SIN DERECHO A CREDITO FISCAL");
        JsonNode prodNc = root.get(5);
        assertThat(prodNc.path("cmd").asText()).isEqualTo("prodNC");
        assertThat(prodNc.path("data").path("imp").asInt()).isEqualTo(1);
        assertThat(prodNc.path("data").path("des01").asText()).isEqualTo("COLGATE TOTAL");
        assertThat(root.get(8).path("cmd").asText()).isEqualTo("endNC");
    }

    @Test
    void setDateRevOPayloadMatchesAnnualInspectionCommand() throws Exception {
        AnnualInspectionInspAo inspAo = AnnualInspectionInspAo.fromChecklist(true, true, true, true, true);
        JsonNode root = objectMapper.readTree(builder.buildSetDateRevOPayload(1_782_259_200L, inspAo));

        assertThat(root.path("cmd").asText()).isEqualTo("SetDateRevO");
        assertThat(root.path("data").asLong()).isEqualTo(1_782_259_200L);
        assertThat(root.path("inspAO").path("precinto").asText()).isEqualTo("Bien");
        assertThat(root.path("inspAO").path("etiqFisc").asText()).isEqualTo("Bien");
        assertThat(root.path("inspAO").path("impFact").asText()).isEqualTo("Bien");
        assertThat(root.path("inspAO").path("impNC").asText()).isEqualTo("Bien");
        assertThat(root.path("inspAO").path("sensPapel").asText()).isEqualTo("Bien");
    }

    @Test
    void creditNotePayloadMatchesFiscalCancellationCommand() throws Exception {
        JsonNode root = objectMapper.readTree(builder.buildCreditNotePayload(
                context(),
                1,
                LocalDate.of(2026, 4, 5)));

        assertThat(root).hasSize(13);
        assertThat(root.get(0).path("cmd").asText()).isEqualTo("nroFacNC");
        assertThat(root.get(0).path("data").asInt()).isEqualTo(1);
        assertThat(root.get(1).path("cmd").asText()).isEqualTo("fechFacNC");
        assertThat(root.get(1).path("data").asText()).isEqualTo("05/04/2026");
        assertThat(root.get(2).path("cmd").asText()).isEqualTo("conSerNC");
        assertThat(root.get(2).path("data").asText()).isEqualTo("GRA0000017");
        assertThat(root.get(3).path("cmd").asText()).isEqualTo("rifCiNC");
        assertThat(root.get(3).path("data").asText()).isEqualTo("J-500662998");
        assertThat(root.get(4).path("cmd").asText()).isEqualTo("razSocNC");
        assertThat(textValues(root.get(4).path("data")))
                .containsExactly("INVERSIONES SHOP COMPUTER 2020, C.A.");

        for (int i = 5; i < 10; i++) {
            JsonNode item = root.get(i);
            assertThat(item.path("cmd").asText()).isEqualTo("prodNC");
            JsonNode data = item.path("data");
            assertThat(data.path("pre").asInt()).isEqualTo(100);
            assertThat(data.path("cant").asInt()).isEqualTo(1000);
            assertThat(data.path("imp").asInt()).isEqualTo(i - 4);
            assertThat(data.path("des01").asText()).isEqualTo("PRODUCTO");
        }

        assertThat(root.get(10).path("cmd").asText()).isEqualTo("endPoNC");
        assertThat(root.get(10).path("data").asInt()).isEqualTo(1);
        assertThat(root.get(10).path("valor").asInt()).isEqualTo(0);
        assertThat(root.get(11).path("cmd").asText()).isEqualTo("fpaNC");
        assertThat(root.get(11).path("data").path("tipo").asInt()).isEqualTo(1);
        assertThat(root.get(11).path("data").path("monto").asInt()).isEqualTo(-1);
        assertThat(root.get(11).path("data").path("tasaConv").asInt()).isEqualTo(0);
        assertThat(root.get(12).path("cmd").asText()).isEqualTo("endNC");
        assertThat(root.get(12).path("data").asInt()).isEqualTo(1);
    }

    @Test
    void reportZPayloadMatchesFiscalCloseCommand() throws Exception {
        JsonNode root = objectMapper.readTree(builder.buildReportZPayload());

        assertThat(root.path("cmd").asText()).isEqualTo("genImpRepZ");
        assertThat(root.path("data").asInt()).isEqualTo(1);
    }

    private static List<String> textValues(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private static List<Integer> intValues(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(JsonNode::asInt)
                .toList();
    }

    private static EnajenacionContext context() {
        return EnajenacionTestContexts.shopComputerContext();
    }
}
