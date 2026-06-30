package com.aeg.core.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FiscalTicketLatin2Test {

    @Test
    void normalizesSpanishAccentsForLatin2() {
        assertThat(FiscalTicketLatin2.normalizeFiscalTicketText("Información técnica"))
                .isEqualTo("Información técnica");
        assertThat(FiscalTicketLatin2.normalizeFiscalTicketText("Niño pequeño"))
                .isEqualTo("Niño pequeño");
    }

    @Test
    void encodesSpanishAccentsAsSingleLatin2Bytes() {
        byte[] bytes = FiscalTicketLatin2.encodePayload("íñ");
        assertThat(bytes).containsExactly((byte) 0xed, (byte) 0xf1);
    }

    @Test
    void encodesSpanishEnyeInFiscalMqttPayload() {
        String payload = "{\"pieFacFijo\":[\"Año nuevo\"]}";
        byte[] bytes = FiscalTicketLatin2.encodeMqttPayload(
                "/20:6E:F1:88:4C:68/AEG_Fiscal/Integracion/Comando",
                payload);

        assertThat(bytes).contains((byte) 0xf1);
        assertThat(bytes).doesNotContain((byte) 0x3f);
    }

    @Test
    void fiscalTopicUsesLatin2BytesInsteadOfUtf8Multibyte() {
        String payload = "{\"line\":\"Sí\"}";
        byte[] bytes = FiscalTicketLatin2.encodeMqttPayload(
                "/20:6E:F1:88:4C:68/AEG_Fiscal/Integracion/Comando",
                payload);

        assertThat(bytes).isNotEqualTo(payload.getBytes(Charset.forName("UTF-8")));
        assertThat(new String(bytes, FiscalTicketLatin2.CHARSET)).isEqualTo(payload);
    }

    @Test
    void normalizesPayloadStringsRecursively() {
        @SuppressWarnings("unchecked")
        Map<String, Object> normalized = (Map<String, Object>) FiscalTicketLatin2.normalizePayloadValue(Map.of(
                "encFacFijo",
                List.of("Línea con acento"),
                "pieFacFijo",
                List.of("Gracias por su compra — vuelva pronto")));

        assertThat(normalized.get("encFacFijo")).isEqualTo(List.of("Línea con acento"));
        assertThat(normalized.get("pieFacFijo")).isEqualTo(List.of("Gracias por su compra - vuelva pronto"));
    }
}
