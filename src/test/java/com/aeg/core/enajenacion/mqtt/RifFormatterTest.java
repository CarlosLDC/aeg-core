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
        assertThat(RifFormatter.isValidFiscalRif("J-508266250")).isTrue();
        assertThat(RifFormatter.isValidFiscalRif("J508266250")).isTrue();
        assertThat(RifFormatter.isValidFiscalRif("J-50826625-0")).isTrue();
        assertThat(RifFormatter.toFiscalForm("J-50826625-0")).isEqualTo("J-508266250");
        assertThat(RifFormatter.isValidFiscalRif("V-005551234")).isTrue();
        assertThat(RifFormatter.isValidFiscalRif("J-50826625")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif("J50826625")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif("V-5551234")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif("INVALID")).isFalse();
        assertThat(RifFormatter.isValidFiscalRif(null)).isFalse();
    }
}
