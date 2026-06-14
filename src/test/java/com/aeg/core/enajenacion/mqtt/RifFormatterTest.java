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
}
