package com.aeg.core.modificationrequest.printer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequest;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionApproveRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionCreateRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRejectRequest;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;

class PrinterEnajenacionRequestServiceImplTest {

    private ModificationRequestRepository repository;
    private PrinterRepository printerRepository;
    private SecurityScopeService securityScope;
    private PrinterEnajenacionRequestServiceImpl service;

    private User distributorUser;
    private User adminUser;
    private Printer printer;

    @BeforeEach
    void setUp() {
        repository = mock(ModificationRequestRepository.class);
        printerRepository = mock(PrinterRepository.class);
        securityScope = mock(SecurityScopeService.class);
        service = new PrinterEnajenacionRequestServiceImpl(repository, printerRepository, securityScope);

        distributorUser = new User();
        distributorUser.setId(2L);
        distributorUser.setName("Distribuidora Demo");
        distributorUser.setRole(Role.DISTRIBUTOR);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("Admin");
        adminUser.setRole(Role.ADMIN);

        printer = new Printer();
        printer.setId(10L);
        printer.setFiscalSerial("GRA0000100");
        printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        printer.setEnajenacionAutorizada(false);

        when(printerRepository.findById(10L)).thenReturn(Optional.of(printer));
        when(printerRepository.save(any(Printer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(ModificationRequest.class))).thenAnswer(inv -> {
            ModificationRequest mr = inv.getArgument(0);
            if (mr.getId() == null) mr.setId(99L);
            return mr;
        });
    }

    @Test
    void createRequestCreatesPendingRequestForEnConsignacionPrinter() {
        when(securityScope.currentUser()).thenReturn(distributorUser);
        when(repository.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.PRINTER_ENAJENACION, 10L, ModificationRequestStatus.PENDING))
                .thenReturn(Optional.empty());

        var res = service.createRequest(new PrinterEnajenacionCreateRequest(10L, 4, "Cliente solicita 4 cuotas"));

        assertThat(res.id()).isEqualTo(99L);
        assertThat(res.status()).isEqualTo(ModificationRequestStatus.PENDING);
        assertThat(res.suggestedInstallments()).isEqualTo(4);
        assertThat(res.notes()).isEqualTo("Cliente solicita 4 cuotas");
    }

    @Test
    void createRequestRejectsNonConsignmentPrinter() {
        printer.setStatus(PrinterStatus.ASIGNADA);
        when(securityScope.currentUser()).thenReturn(distributorUser);

        assertThatThrownBy(() -> service.createRequest(new PrinterEnajenacionCreateRequest(10L, 3, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approveAuthorizesPrinterAndSetsCreditInstallments() {
        when(securityScope.currentUser()).thenReturn(adminUser);
        ModificationRequest mr = new ModificationRequest();
        mr.setId(99L);
        mr.setTargetType(ModificationTargetType.PRINTER_ENAJENACION);
        mr.setTargetId(10L);
        mr.setActionType(ModificationActionType.UPDATE);
        mr.setStatus(ModificationRequestStatus.PENDING);
        mr.setRequestedBy(distributorUser);
        when(repository.findById(99L)).thenReturn(Optional.of(mr));

        var res = service.approve(99L, new PrinterEnajenacionApproveRequest(6, new BigDecimal("450.00"), "Aprobado"));

        assertThat(res.status()).isEqualTo(ModificationRequestStatus.APPROVED);
        assertThat(res.approvedInstallments()).isEqualTo(6);
        assertThat(printer.getEnajenacionAutorizada()).isTrue();
        assertThat(printer.getEnajenacionAutorizadaBy()).isEqualTo(1L);
        assertThat(printer.getCreditInstallments()).isEqualTo(6);
        assertThat(printer.getFinalSalePrice()).isEqualByComparingTo("450.00");
        verify(printerRepository).save(printer);
    }

    @Test
    void approveRejectsDistributorUser() {
        when(securityScope.currentUser()).thenReturn(distributorUser);

        assertThatThrownBy(() -> service.approve(99L, new PrinterEnajenacionApproveRequest(3, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectMarksRequestRejectedWithoutAuthorizingPrinter() {
        when(securityScope.currentUser()).thenReturn(adminUser);
        ModificationRequest mr = new ModificationRequest();
        mr.setId(99L);
        mr.setTargetType(ModificationTargetType.PRINTER_ENAJENACION);
        mr.setTargetId(10L);
        mr.setActionType(ModificationActionType.UPDATE);
        mr.setStatus(ModificationRequestStatus.PENDING);
        mr.setRequestedBy(distributorUser);
        when(repository.findById(99L)).thenReturn(Optional.of(mr));

        var res = service.reject(99L, new PrinterEnajenacionRejectRequest("Saldo vencido"));

        assertThat(res.status()).isEqualTo(ModificationRequestStatus.REJECTED);
        assertThat(res.reviewNote()).isEqualTo("Saldo vencido");
        assertThat(printer.getEnajenacionAutorizada()).isFalse();
    }
}
