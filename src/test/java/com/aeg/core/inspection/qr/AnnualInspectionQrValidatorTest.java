package com.aeg.core.inspection.qr;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;

class AnnualInspectionQrValidatorTest {

    private static final String EXAMPLE_QR =
            "ZSn8njvkbk7x+iu8IOFJD+OXWW65uuvLX79us586JYrENbi5Z8LiNvllg9bhB/ca";
    private static final String SECRET = "AeGsGrAsFeCh2024";

    private PrinterRepository printerRepository;
    private AnnualInspectionQrValidator validator;
    private AnnualInspectionQrPayload examplePayload;

    @BeforeEach
    void setUp() {
        AnnualInspectionQrSettings settings = new AnnualInspectionQrSettings();
        ReflectionTestUtils.setField(settings, "secret", SECRET);
        AnnualInspectionQrDecoder decoder = new AnnualInspectionQrDecoder(settings);
        examplePayload = decoder.decode(EXAMPLE_QR);

        printerRepository = mock(PrinterRepository.class);
        validator = new AnnualInspectionQrValidator(printerRepository, decoder);
    }

    @Test
    void rejectsRegistroMismatch() {
        Printer printer = printerWithMac(examplePayload.mac());
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));

        assertThatThrownBy(() -> validator.decodeAndValidate(1L, EXAMPLE_QR, "GRA9999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registro");
    }

    @Test
    void rejectsMacMismatch() {
        Printer printer = printerWithMac("AA:BB:CC:DD:EE:FF");
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));

        assertThatThrownBy(() -> validator.decodeAndValidate(
                        1L,
                        EXAMPLE_QR,
                        examplePayload.registro()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAC");
    }

    @Test
    void acceptsMatchingRegistroAndMac() {
        Printer printer = printerWithMac(examplePayload.mac());
        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));

        AnnualInspectionQrPayload result = validator.decodeAndValidate(
                1L,
                EXAMPLE_QR,
                examplePayload.registro());

        org.assertj.core.api.Assertions.assertThat(result.registro()).isEqualTo(examplePayload.registro());
        org.assertj.core.api.Assertions.assertThat(result.mac()).isEqualTo(examplePayload.mac());
    }

    private static Printer printerWithMac(String mac) {
        Printer printer = mock(Printer.class);
        when(printer.getMacAddress()).thenReturn(mac);
        when(printer.getId()).thenReturn(1L);
        return printer;
    }
}
