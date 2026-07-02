package com.aeg.core.inspection.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

class AnnualInspectionQrLookupServiceTest {

    private static final String EXAMPLE_QR =
            "ZSn8njvkbk7x+iu8IOFJD+OXWW65uuvLX79us586JYrENbi5Z8LiNvllg9bhB/ca";
    private static final String SECRET = "AeGsGrAsFeCh2024";

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

        inspectionRepository = mock(AnnualInspectionRepository.class);
        securityScope = mock(SecurityScopeService.class);
        lookupService = new AnnualInspectionQrLookupService(
                decoder,
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
        inspection.setInspectionDate(LocalDate.now());
        inspection.setCreatedAt(OffsetDateTime.now());
        inspection.setMqttRegistroImpresora("GRA0000017");

        when(securityScope.findVisiblePrinters()).thenReturn(List.of(printer));
    }

    @Test
    void findsInspectionByRegistroAndMac() {
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
    }

    @Test
    void prefersExactQrCodigoMatch() {
        when(inspectionRepository.findByPrinter_IdAndMqttQrCodigo(17L, EXAMPLE_QR))
                .thenReturn(Optional.of(inspection));

        FiscalBookLookupInspectionByQrResponse response = lookupService.lookup(EXAMPLE_QR);

        assertThat(response.inspectionId()).isEqualTo(42L);
    }

    @Test
    void throwsWhenNoInspectionMatches() {
        when(inspectionRepository.findByPrinter_IdAndMqttQrCodigo(17L, EXAMPLE_QR))
                .thenReturn(Optional.empty());
        when(inspectionRepository.findByPrinter_IdAndMqttRegistroImpresoraIgnoreCase(17L, "GRA0000017"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> lookupService.lookup(EXAMPLE_QR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(AnnualInspectionQrMessages.INVALID_CODE);
    }

    @Test
    void throwsWhenPrinterNotFoundByMac() {
        when(securityScope.findVisiblePrinters()).thenReturn(List.of());

        assertThatThrownBy(() -> lookupService.lookup(EXAMPLE_QR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(AnnualInspectionQrMessages.INVALID_CODE);
    }

    @Test
    void trimsSecretBeforeDecoding() {
        AnnualInspectionQrSettings settings = new AnnualInspectionQrSettings();
        ReflectionTestUtils.setField(settings, "secret", SECRET + " ");
        AnnualInspectionQrDecoder decoder = new AnnualInspectionQrDecoder(settings);
        AnnualInspectionQrLookupService service = new AnnualInspectionQrLookupService(
                decoder,
                inspectionRepository,
                securityScope);

        when(inspectionRepository.findByPrinter_IdAndMqttQrCodigo(17L, EXAMPLE_QR))
                .thenReturn(Optional.empty());
        when(inspectionRepository.findByPrinter_IdAndMqttRegistroImpresoraIgnoreCase(17L, "GRA0000017"))
                .thenReturn(List.of(inspection));

        FiscalBookLookupInspectionByQrResponse response = service.lookup(EXAMPLE_QR);

        assertThat(response.inspectionId()).isEqualTo(42L);
        verify(securityScope).findVisiblePrinters();
    }
}
