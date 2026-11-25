package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RifFormatterTest {

    @Test
    void insertsHyphenAfterLetter() {
        assertThat(RifFormatter.toFiscalForm("J500662998")).isEqualTo("J-500662998");
    }

    @Test
    void keepsExistingHyphen() {
        assertThat(RifFormatter.toFiscalForm("J-500662998")).isEqualTo("J-500662998");
    }

    @Test
    void isValidFiscalRifRequiresNineDigits() {
        assertThat(RifFormatter.isValidFiscalRif("J-508266251")).isTrue();
        assertThat(RifFormatter.isValidFiscalRif("J508266251")).isTrue();
        assertThat(RifFormatter.isValidFiscalRif("V-123456789")).isTrue();
        assertThat(RifFormatter.isValidFiscalRif("J-50826625")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif("J50826625")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif("J-0000001")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif("INVALID")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif(null)).isFalse();
    }
}
