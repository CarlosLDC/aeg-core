package com.aeg.core.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrinterEncryptionKeyGeneratorTest {

    @Test
    void generatesExpectedKeyForKnownValues() {
        // GRA0000001 | 3C:DC:75:64:97:C0 -> MD5 input = "GRA0000001|3C:DC:75:64:97:C0"
        // MD5 full = "f53dad33c38e0fbdcb05fd47f4bd9ff2"
        // Substring(0, 16) = "f53dad33c38e0fbd"
        String key = PrinterEncryptionKeyGenerator.generateKey("GRA0000001", "3C:DC:75:64:97:C0");
        assertThat(key).isEqualTo("f53dad33c38e0fbd");
        assertThat(key).hasSize(16);
    }

    @Test
    void normalizesLowercaseAndWhitespace() {
        String key = PrinterEncryptionKeyGenerator.generateKey("  gra0000001  ", "  3c:dc:75:64:97:c0  ");
        assertThat(key).isEqualTo("f53dad33c38e0fbd");
    }

    @Test
    void normalizesCompactMacAddress() {
        String key = PrinterEncryptionKeyGenerator.generateKey("GRA0000001", "3cdc756497c0");
        assertThat(key).isEqualTo("f53dad33c38e0fbd");
    }

    @Test
    void returnsNullWhenInputsAreMissing() {
        assertThat(PrinterEncryptionKeyGenerator.generateKey(null, "3C:DC:75:64:97:C0")).isNull();
        assertThat(PrinterEncryptionKeyGenerator.generateKey("", "3C:DC:75:64:97:C0")).isNull();
        assertThat(PrinterEncryptionKeyGenerator.generateKey("GRA0000001", null)).isNull();
        assertThat(PrinterEncryptionKeyGenerator.generateKey("GRA0000001", "")).isNull();
        assertThat(PrinterEncryptionKeyGenerator.generateKey("   ", "   ")).isNull();
    }
}
