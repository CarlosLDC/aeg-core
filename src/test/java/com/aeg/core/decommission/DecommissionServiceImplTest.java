package com.aeg.core.decommission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.abnormality.AbnormalityOrigin;
import com.aeg.core.abnormality.AbnormalityService;
import com.aeg.core.abnormality.AbnormalityType;
import com.aeg.core.decommission.dto.DecommissionRequest;
import com.aeg.core.decommission.dto.DecommissionResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

class DecommissionServiceImplTest {

    private PrinterDecommissionRepository repository;
    private PrinterRepository printerRepository;
    private AbnormalityService abnormalityService;
    private DecommissionServiceImpl service;
    private Printer printer;

    @BeforeEach
    void setUp() {
        repository = mock(PrinterDecommissionRepository.class);
        printerRepository = mock(PrinterRepository.class);
        abnormalityService = mock(AbnormalityService.class);
        SecurityScopeService securityScope = mock(SecurityScopeService.class);
        when(securityScope.currentUser()).thenReturn(new User());
        service = new DecommissionServiceImpl(repository, printerRepository, securityScope, abnormalityService);

        printer = new Printer();
        printer.setId(5L);
        printer.setStatus(PrinterStatus.ENAJENADA);
        when(printerRepository.findById(5L)).thenReturn(Optional.of(printer));
        when(repository.save(any(PrinterDecommission.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void decommissionChangesStatusAndStoresRecord() {
        DecommissionResponse response = service.decommission(5L,
                new DecommissionRequest(DecommissionReason.DESUSO, null, false, "obsoleta"));

        assertThat(printer.getStatus()).isEqualTo(PrinterStatus.DESINCORPORADA);
        assertThat(response.reason()).isEqualTo(DecommissionReason.DESUSO);
        assertThat(response.printerId()).isEqualTo(5L);
        verify(printerRepository).save(printer);
        verify(abnormalityService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void lossAlsoRecordsAbnormality() {
        service.decommission(5L, new DecommissionRequest(DecommissionReason.PERDIDA, null, true, "robo"));

        verify(abnormalityService).record(
                any(Printer.class), org.mockito.ArgumentMatchers.eq(AbnormalityType.PERDIDA),
                org.mockito.ArgumentMatchers.eq(AbnormalityOrigin.MANUAL), any(), any(), org.mockito.ArgumentMatchers.eq("robo"));
    }

    @Test
    void rejectsAlreadyDecommissionedPrinter() {
        printer.setStatus(PrinterStatus.DESINCORPORADA);

        assertThatThrownBy(() -> service.decommission(5L,
                new DecommissionRequest(DecommissionReason.OTRO, null, false, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unknownPrinterThrowsNotFound() {
        when(printerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decommission(99L,
                new DecommissionRequest(DecommissionReason.OTRO, null, false, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
