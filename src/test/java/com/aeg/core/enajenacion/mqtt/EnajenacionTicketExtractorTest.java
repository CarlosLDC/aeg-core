package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aeg.core.company.ContributorType;

class EnajenacionTicketExtractorTest {

    @Test
    void extractHeaderLinesOmitsBlankSecondAddressLine() {
        List<String> encabezado = List.of(
                "SENIAT",
                "J-503752890",
                "ABASTO HERMANOS YEISAR 2023, C.A.",
                "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                "",
                "PUERTO LA CRUZ, ANZOATEGUI");

        assertThat(EnajenacionTicketExtractor.extractHeaderLines(encabezado, ContributorType.ORDINARIO))
                .containsExactly(
                        "AV SANTA CRUZ LOCAL NRO 13 SECTOR POZUELOS",
                        "PUERTO LA CRUZ, ANZOATEGUI",
                        "CONTRIBUYENTE ORDINARIO");
    }

    @Test
    void extractHeaderLinesKeepsSplitAddress() {
        List<String> encabezado = List.of(
                "SENIAT",
                "J-500662998",
                "INVERSIONES SHOP COMPUTER 2020, C.A.",
                "AV. URDANETA EDIF. CASA BERA",
                "PISO PB LOCAL -005-C URB. LA CANDELARIA",
                "CARACAS, DISTRITO CAPITAL");

        assertThat(EnajenacionTicketExtractor.extractHeaderLines(encabezado, ContributorType.ORDINARIO))
                .containsExactly(
                        "AV. URDANETA EDIF. CASA BERA",
                        "PISO PB LOCAL -005-C URB. LA CANDELARIA",
                        "CARACAS, DISTRITO CAPITAL",
                        "CONTRIBUYENTE ORDINARIO");
    }

    @Test
    void extractTrailerLinesFiltersBlankLines() {
        assertThat(EnajenacionTicketExtractor.extractTrailerLines(List.of("PIE 01", " ", "PIE 02")))
                .containsExactly("PIE 01", "PIE 02");
    }

    @Test
    void extractHeaderLinesRejectsMissingAddressBlock() {
        assertThatThrownBy(() -> EnajenacionTicketExtractor.extractHeaderLines(
                        List.of("SENIAT", "J-1", "EMPRESA"), ContributorType.ORDINARIO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
