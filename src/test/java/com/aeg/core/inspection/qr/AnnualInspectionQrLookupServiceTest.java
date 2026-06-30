package com.aeg.core.inspection.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.aeg.core.fiscalbook.dto.FiscalBookLookupInspectionByQrResponse;
import com.aeg.core.inspection.AnnualInspection;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

class AnnualInspectionQrLookupServiceTest {

    private static final String EXAMPLE_QR =
            "ZSn8njvkbk7x+iu8IOFJD+OXWW65uuvLX79us586JYrENbi5Z8LiNvllg9bhB/ca";
    private static final String SECRET = "AeGsGrAsFeCh2024";

    private PrinterRepository printerRepository;
    private AnnualInspectionRepository inspectionRepository;
    private SecurityScopeService securityScope;
    private AnnualInspectionQrLookupService lookupService;
    private Printer printer;
    private AnnualInspection inspection;

    @BeforeEach
    void setUp() {
        AnnualInspectionQrSettings settings = new AnnualInspectionQrSettings();
        ReflectionTestUtils.setField(settings, "secret", SECRET);
        AnnualInspectionQrDecoder decoder = new AnnualInspectionQrDecoder(settings);

        printerRepository = mock(PrinterRepository.class);
        inspectionRepository = mock(AnnualInspectionRepository.class);
        securityScope = mock(SecurityScopeService.class);
        lookupService = new AnnualInspectionQrLookupService(
                decoder,
                printerRepository,
                inspectionRepository,
                securityScope);

        printer = new Printer();
        printer.setId(17L);
        printer.setFiscalSerial("ABC1234567");
        printer.setMacAddress("20:6E:F1:88:4C:68");

        inspection = new AnnualInspection();
        inspection.setId(42L);
        inspection.setPrinter(printer);
        inspection.setInspectorUser(mock(com.aeg.core.security.User.class));
        inspection.setSealTampered(false);
        inspection.setPhotoUrls(new String[0]);
        inspection.setInspectionDate(LocalDate.now());
        inspection.setCreatedAt(OffsetDateTime.now());
        inspection.setMqttRegistroImpresora("GRA0000017");
    }

    @Test
    void findsInspectionByRegistroAndMac() {
        when(printerRepository.findByMacAddressCompact("206EF1884C68"))
                .thenReturn(Optional.of(printer));
        when(inspectionRepository.findByPrinter_IdAndMqttQrCodigo(17L, EXAMPLE_QR))
                .thenReturn(Optional.empty());
        when(inspectionRepository.findByPrinter_IdAndMqttRegistroImpresoraIgnoreCase(17L, "GRA0000017"))
                .thenReturn(List.of(inspection));

        FiscalBookLookupInspectionByQrResponse response = lookupService.lookup(EXAMPLE_QR);

        assertThat(response.inspectionId()).isEqualTo(42L);
        assertThat(response.printerId()).isEqualTo(17L);
        assertThat(response.registro()).isEqualTo("GRA0000017");
        assertThat(response.mac()).isEqualTo("20:6E:F1:88:4C:68");
        assertThat(response.fecha()).isEqualTo("29/06/2026");
        verify(securityScope).assertPrinterInScope(printer);
    }

    @Test
    void prefersExactQrCodigoMatch() {
        when(printerRepository.findByMacAddressCompact("206EF1884C68"))
                .thenReturn(Optional.of(printer));
        when(inspectionRepository.findByPrinter_IdAndMqttQrCodigo(17L, EXAMPLE_QR))
                .thenReturn(Optional.of(inspection));

        FiscalBookLookupInspectionByQrResponse response = lookupService.lookup(EXAMPLE_QR);

        assertThat(response.inspectionId()).isEqualTo(42L);
    }

    @Test
    void throwsWhenNoInspectionMatches() {
        when(printerRepository.findByMacAddressCompact("206EF1884C68"))
                .thenReturn(Optional.of(printer));
        when(inspectionRepository.findByPrinter_IdAndMqttQrCodigo(17L, EXAMPLE_QR))
                .thenReturn(Optional.empty());
        when(inspectionRepository.findByPrinter_IdAndMqttRegistroImpresoraIgnoreCase(17L, "GRA0000017"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> lookupService.lookup(EXAMPLE_QR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe un registro");
    }

    @Test
    void throwsWhenPrinterNotFoundByMac() {
        when(printerRepository.findByMacAddressCompact(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookupService.lookup(EXAMPLE_QR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MAC");
    }
}
