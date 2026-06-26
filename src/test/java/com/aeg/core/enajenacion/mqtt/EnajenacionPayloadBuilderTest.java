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
                "PUERTO LA CRUZ, ANZOATEGUI");

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
        return new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J500662998",
                "INVERSIONES SHOP COMPUTER 2020, C.A.",
                "CONTRIBUYENTE ORDINARIO",
                "AV. URDANETA EDIF. CASA BERA",
                "PISO PB LOCAL -005-C URB. LA CANDELARIA",
                "CARACAS, DISTRITO CAPITAL");
    }
}
