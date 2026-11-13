package com.aeg.core.printer.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printer.credit.dto.PrinterCreditPaymentRequest;
import com.aeg.core.printer.credit.dto.PrinterCreditPlanRequest;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;

class PrinterCreditServiceImplTest {

    private PrinterRepository printerRepository;
    private PrinterCreditPaymentRepository paymentRepository;
    private SecurityScopeService securityScope;
    private PrinterCreditServiceImpl service;

    private User adminUser;
    private User distributorUser;
    private Printer printer;

    @BeforeEach
    void setUp() {
        printerRepository = mock(PrinterRepository.class);
        paymentRepository = mock(PrinterCreditPaymentRepository.class);
        securityScope = mock(SecurityScopeService.class);
        service = new PrinterCreditServiceImpl(printerRepository, paymentRepository, securityScope);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("Admin");
        adminUser.setRole(Role.ADMIN);

        distributorUser = new User();
        distributorUser.setId(2L);
        distributorUser.setRole(Role.DISTRIBUTOR);

        printer = new Printer();
        printer.setId(20L);
        printer.setFiscalSerial("GRA0000200");
        printer.setStatus(PrinterStatus.A_CREDITO);
        printer.setPaid(false);
        printer.setCreditInstallments(4);
        printer.setFinalSalePrice(new BigDecimal("400.00"));

        when(printerRepository.findById(20L)).thenReturn(Optional.of(printer));
        when(printerRepository.save(any(Printer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(PrinterCreditPayment.class))).thenAnswer(inv -> {
            PrinterCreditPayment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(50L);
            return p;
        });
    }

    @Test
    void updateCreditPlanUpdatesInstallmentsAndPrice() {
        when(securityScope.currentUser()).thenReturn(adminUser);
        when(paymentRepository.findByPrinter_IdOrderByPaymentDateDescIdDesc(20L)).thenReturn(List.of());

        var summary = service.updateCreditPlan(20L, new PrinterCreditPlanRequest(6, new BigDecimal("600.00")));

        assertThat(summary.creditInstallments()).isEqualTo(6);
        assertThat(summary.finalSalePrice()).isEqualByComparingTo("600.00");
        assertThat(summary.remainingBalance()).isEqualByComparingTo("600.00");
    }

    @Test
    void registerPaymentRecordsPaymentAndCanMarkAsPaid() {
        when(securityScope.currentUser()).thenReturn(adminUser);
        PrinterCreditPayment savedPayment = new PrinterCreditPayment();
        savedPayment.setId(50L);
        savedPayment.setPrinter(printer);
        savedPayment.setAmount(new BigDecimal("400.00"));
        savedPayment.setPaymentDate(LocalDate.of(2026, 11, 13));
        savedPayment.setRegisteredBy(adminUser);
        when(paymentRepository.findByPrinter_IdOrderByPaymentDateDescIdDesc(20L))
                .thenReturn(List.of(savedPayment));

        var summary = service.registerPayment(
                20L,
                new PrinterCreditPaymentRequest(
                        new BigDecimal("400.00"),
                        LocalDate.of(2026, 11, 13),
                        "REF-001",
                        "Pago total",
                        true));

        assertThat(printer.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(printer.getPaid()).isTrue();
        assertThat(summary.totalPaid()).isEqualByComparingTo("400.00");
        assertThat(summary.remainingBalance()).isEqualByComparingTo("0.00");
        assertThat(summary.paymentCount()).isEqualTo(1);
        verify(paymentRepository).save(any(PrinterCreditPayment.class));
    }

    @Test
    void registerPaymentRejectsDistributorUser() {
        when(securityScope.currentUser()).thenReturn(distributorUser);

        assertThatThrownBy(() -> service.registerPayment(
                20L,
                new PrinterCreditPaymentRequest(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 13),
                        null,
                        null,
                        false)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
