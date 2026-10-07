package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;

@ExtendWith(MockitoExtension.class)
class EnajenacionCompletionServiceTest {

    @Mock
    private PrinterRepository printerRepository;

    private EnajenacionCompletionService service;

    @BeforeEach
    void setUp() {
        service = new EnajenacionCompletionService(printerRepository);
    }

    @Test
    void markEnajenadaSetsStatusAndEncryptionKey() {
        Printer printer = new Printer();
        printer.setId(1L);
        printer.setStatus(PrinterStatus.ASIGNADA);
        printer.setFiscalSerial("GRA0000017");

        when(printerRepository.findById(1L)).thenReturn(Optional.of(printer));

        service.markEnajenada(1L, "55a42534f4d2d8b9");

        assertThat(printer.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(printer.getEncryptionKey()).isEqualTo("55a42534f4d2d8b9");
        assertThat(printer.getInstallationDate()).isNotNull();
        verify(printerRepository).save(printer);
    }

    @Test
    void markEnajenadaAlreadyEnajenadaUpdatesKeyIfProvided() {
        Printer printer = new Printer();
        printer.setId(2L);
        printer.setStatus(PrinterStatus.ENAJENADA);
        printer.setFiscalSerial("GRA0000018");

        when(printerRepository.findById(2L)).thenReturn(Optional.of(printer));

        service.markEnajenada(2L, "55a42534f4d2d8b9");

        assertThat(printer.getEncryptionKey()).isEqualTo("55a42534f4d2d8b9");
        verify(printerRepository).save(printer);
    }

    @Test
    void markEnajenadaAutogeneratesKeyWhenNull() {
        Printer printer = new Printer();
        printer.setId(3L);
        printer.setStatus(PrinterStatus.ASIGNADA);
        printer.setFiscalSerial("GRA0000017");
        printer.setMacAddress("20:6E:F1:88:4C:68");

        when(printerRepository.findById(3L)).thenReturn(Optional.of(printer));

        service.markEnajenada(3L, null);

        assertThat(printer.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(printer.getEncryptionKey()).isEqualTo("482266916ca28364");
        verify(printerRepository).save(printer);
    }
}
