package com.aeg.core.abnormality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;

class AbnormalityServiceImplTest {

    private AbnormalityRepository repository;
    private SecurityScopeService securityScope;
    private AbnormalityServiceImpl service;
    private Printer printer;

    @BeforeEach
    void setUp() {
        repository = mock(AbnormalityRepository.class);
        securityScope = mock(SecurityScopeService.class);
        service = new AbnormalityServiceImpl(repository, mock(PrinterRepository.class), securityScope);
        printer = new Printer();
        printer.setId(7L);
    }

    @Test
    void recordCreatesAbnormalityWithOrigin() {
        when(repository.existsByOriginAndOriginIdAndType(
                AbnormalityOrigin.INSPECCION, 10L, AbnormalityType.PRECINTO_ALTERADO)).thenReturn(false);

        service.record(printer, AbnormalityType.PRECINTO_ALTERADO, AbnormalityOrigin.INSPECCION, 10L, null, "x");

        ArgumentCaptor<Abnormality> captor = ArgumentCaptor.forClass(Abnormality.class);
        verify(repository).save(captor.capture());
        Abnormality saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(AbnormalityType.PRECINTO_ALTERADO);
        assertThat(saved.getOrigin()).isEqualTo(AbnormalityOrigin.INSPECCION);
        assertThat(saved.getOriginId()).isEqualTo(10L);
        assertThat(saved.getPrinterId()).isEqualTo(7L);
    }

    @Test
    void recordIsIdempotentForSameOriginAndType() {
        when(repository.existsByOriginAndOriginIdAndType(
                AbnormalityOrigin.SERVICIO, 3L, AbnormalityType.PRECINTO_ALTERADO)).thenReturn(true);

        service.record(printer, AbnormalityType.PRECINTO_ALTERADO, AbnormalityOrigin.SERVICIO, 3L, null, "x");

        verify(repository, never()).save(any());
    }

    @Test
    void findAllReturnsEmptyWhenNoVisiblePrinters() {
        when(securityScope.isGlobalReader()).thenReturn(false);
        when(securityScope.visiblePrinterIds()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
        verify(repository, never()).findByPrinter_IdInOrderByDetectedAtDesc(any());
    }
}
