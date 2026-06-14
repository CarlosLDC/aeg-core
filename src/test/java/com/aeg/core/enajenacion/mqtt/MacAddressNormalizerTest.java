package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MacAddressNormalizerTest {

    @Test
    void convertsCompactToColonForm() {
        assertThat(MacAddressNormalizer.toColonForm("206EF1884C68")).isEqualTo("20:6E:F1:88:4C:68");
    }

    @Test
    void convertsColonToCompactForm() {
        assertThat(MacAddressNormalizer.toCompactForm("20:6E:F1:88:4C:68")).isEqualTo("206EF1884C68");
    }

    @Test
    void comparesMacIgnoringSeparatorsAndCase() {
        assertThat(MacAddressNormalizer.sameMac("206ef1884c68", "20:6E:F1:88:4C:68")).isTrue();
    }
}
