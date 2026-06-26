package com.aeg.core.fiscal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FiscalInvoiceProductDescriptionTest {

  @Test
  void singleLineDescriptionIsLimitedTo39Characters() {
    String longLine = "A".repeat(50);

    List<String> lines = FiscalInvoiceProductDescription.resolveLines(longLine, "PRODUCTO");

    assertThat(lines).containsExactly("A".repeat(39));
    assertThat(FiscalInvoiceProductDescription.linesForProfCommands(lines))
        .containsOnly("A".repeat(39));
  }

  @Test
  void multiLineDescriptionUses60CharactersPerLine() {
    String input = "L1".repeat(40) + "\n" + "L2".repeat(40);

    List<String> lines = FiscalInvoiceProductDescription.resolveLines(input, "PRODUCTO");

    assertThat(lines).hasSize(2);
    assertThat(lines.get(0)).hasSize(60);
    assertThat(lines.get(1)).hasSize(60);
    assertThat(FiscalInvoiceProductDescription.linesForProfCommands(lines))
        .containsExactly(lines.get(0), lines.get(1), "", "", "");
  }

  @Test
  void defaultDescriptionFitsSingleLineLimit() {
    List<String> lines = FiscalInvoiceProductDescription.resolveLines(null, "PRODUCTO");

    assertThat(lines).containsExactly("PRODUCTO");
  }
}
