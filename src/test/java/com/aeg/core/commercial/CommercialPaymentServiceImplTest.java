package com.aeg.core.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.commercial.dto.CommercialPaymentRequest;
import com.aeg.core.company.Company;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;

class CommercialPaymentServiceImplTest {

    private PagoComercialRepository pagoRepository;
    private FacturaComercialRepository facturaRepository;
    private PrinterRepository printerRepository;
    private SecurityScopeService securityScope;
    private CommercialPaymentServiceImpl service;
    private FacturaComercial invoice;
    private User adminUser;

    @BeforeEach
    void setUp() {
        pagoRepository = mock(PagoComercialRepository.class);
        facturaRepository = mock(FacturaComercialRepository.class);
        printerRepository = mock(PrinterRepository.class);
        securityScope = mock(SecurityScopeService.class);
        service = new CommercialPaymentServiceImpl(
                pagoRepository, facturaRepository, printerRepository, securityScope);

        adminUser = new User();
        adminUser.setId(9L);
        adminUser.setName("Admin Test");
        adminUser.setRole(Role.ADMIN);
        when(securityScope.currentUser()).thenReturn(adminUser);

        Company company = new Company();
        company.setId(7L);
        company.setBusinessName("Demo Empresa");

        invoice = new FacturaComercial();
        try {
            var idField = FacturaComercial.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoice, 55L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        invoice.setNumeroFactura("FAC-055");
        invoice.setCompany(company);
        invoice.setModalidad("VENTA");
        invoice.setEstadoCobro("PENDIENTE_PAGO");

        when(facturaRepository.findById(55L)).thenReturn(Optional.of(invoice));
        when(facturaRepository.save(any(FacturaComercial.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoRepository.save(any(PagoComercial.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_withMarkInvoiceAsPaid_setsInvoicePaidAndSettlesCreditPrinters() {
        Printer creditPrinter = new Printer();
        creditPrinter.setId(101L);
        creditPrinter.setStatus(PrinterStatus.A_CREDITO);
        creditPrinter.setPaid(false);

        when(printerRepository.findByFacturaComercial_IdAndStatus(55L, PrinterStatus.A_CREDITO))
                .thenReturn(List.of(creditPrinter));

        CommercialPaymentRequest request = new CommercialPaymentRequest();
        request.setFacturaComercialId(55L);
        request.setMetodoBanco("ZELLE");
        request.setMonto(new BigDecimal("450.00"));
        request.setReferencia("Z-9001");
        request.setMarkInvoiceAsPaid(true);

        service.create(request);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PAGADO");
        assertThat(creditPrinter.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(creditPrinter.getPaid()).isTrue();
        verify(facturaRepository).save(invoice);
        verify(printerRepository).saveAll(List.of(creditPrinter));
    }

    @Test
    void create_withPositiveAmountOnPendingInvoice_movesToPagoParcialWithoutSettling() {
        CommercialPaymentRequest request = new CommercialPaymentRequest();
        request.setFacturaComercialId(55L);
        request.setMetodoBanco("BNC");
        request.setMonto(new BigDecimal("150.00"));
        request.setMarkInvoiceAsPaid(false);

        service.create(request);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PAGO_PARCIAL");
        verify(facturaRepository).save(invoice);
        verify(printerRepository, never()).findByFacturaComercial_IdAndStatus(any(), any());
    }

    @Test
    void create_onPagoParcialWithoutMarkAsPaid_leavesEstadoCobroUnchanged() {
        invoice.setEstadoCobro("PAGO_PARCIAL");

        CommercialPaymentRequest request = new CommercialPaymentRequest();
        request.setFacturaComercialId(55L);
        request.setMetodoBanco("BINANCE");
        request.setMonto(new BigDecimal("100.00"));

        service.create(request);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PAGO_PARCIAL");
        verify(facturaRepository, never()).save(any());
        verify(printerRepository, never()).findByFacturaComercial_IdAndStatus(any(), any());
    }

    @Test
    void create_populatesFechaPagoNotasRegisteredByAndEmpresaId() {
        CommercialPaymentRequest request = new CommercialPaymentRequest();
        request.setFacturaComercialId(55L);
        request.setMetodoBanco("ZELLE");
        request.setMonto(new BigDecimal("120.00"));
        request.setFechaPago(LocalDate.of(2026, 11, 20));
        request.setNotas("  Abono cuota 1  ");

        var response = service.create(request);

        assertThat(response.getFechaPago()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(response.getNotas()).isEqualTo("Abono cuota 1");
        assertThat(response.getRegisteredById()).isEqualTo(9L);
        assertThat(response.getRegisteredByName()).isEqualTo("Admin Test");
        assertThat(response.getEmpresaId()).isEqualTo(7L);
    }

    @Test
    void create_defaultsFechaPagoToTodayWhenOmitted() {
        CommercialPaymentRequest request = new CommercialPaymentRequest();
        request.setFacturaComercialId(55L);
        request.setMetodoBanco("BNC");
        request.setMonto(new BigDecimal("80.00"));

        var response = service.create(request);

        assertThat(response.getFechaPago()).isEqualTo(LocalDate.now());
    }

    @Test
    void delete_revertsPagoParcialToPendientePagoWhenNoPaymentsRemain() {
        invoice.setEstadoCobro("PAGO_PARCIAL");
        PagoComercial payment = new PagoComercial();
        payment.setFacturaComercial(invoice);
        payment.setMetodoBanco("BNC");
        payment.setMonto(new BigDecimal("100.00"));

        when(pagoRepository.findById(70L)).thenReturn(Optional.of(payment));
        when(pagoRepository.countByFacturaComercial_Id(55L)).thenReturn(0L);

        service.delete(70L);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PENDIENTE_PAGO");
        verify(pagoRepository).delete(payment);
        verify(pagoRepository).flush();
        verify(facturaRepository).save(invoice);
    }

    @Test
    void delete_keepsPagoParcialWhenOtherPaymentsRemainAndDoesNotRevertPagado() {
        invoice.setEstadoCobro("PAGO_PARCIAL");
        PagoComercial payment = new PagoComercial();
        payment.setFacturaComercial(invoice);
        payment.setMetodoBanco("BNC");

        when(pagoRepository.findById(71L)).thenReturn(Optional.of(payment));
        when(pagoRepository.countByFacturaComercial_Id(55L)).thenReturn(1L);

        service.delete(71L);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PAGO_PARCIAL");
        verify(facturaRepository, never()).save(any());

        invoice.setEstadoCobro("PAGADO");
        when(pagoRepository.countByFacturaComercial_Id(55L)).thenReturn(0L);
        service.delete(71L);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PAGADO");
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void update_movingOnlyPaymentToAnotherInvoiceRevertsSourceAndPromotesTarget() throws Exception {
        invoice.setEstadoCobro("PAGO_PARCIAL");
        FacturaComercial targetInvoice = new FacturaComercial();
        var idField = FacturaComercial.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(targetInvoice, 66L);
        targetInvoice.setNumeroFactura("FAC-066");
        targetInvoice.setEstadoCobro("PENDIENTE_PAGO");
        when(facturaRepository.findById(66L)).thenReturn(Optional.of(targetInvoice));

        PagoComercial payment = new PagoComercial();
        payment.setFacturaComercial(invoice);
        payment.setMetodoBanco("ZELLE");
        payment.setMonto(new BigDecimal("200.00"));
        when(pagoRepository.findById(72L)).thenReturn(Optional.of(payment));
        when(pagoRepository.countByFacturaComercial_Id(55L)).thenReturn(0L);

        CommercialPaymentRequest moveReq = new CommercialPaymentRequest();
        moveReq.setFacturaComercialId(66L);
        moveReq.setMetodoBanco("ZELLE");
        moveReq.setMonto(new BigDecimal("200.00"));

        service.update(72L, moveReq);

        assertThat(invoice.getEstadoCobro()).isEqualTo("PENDIENTE_PAGO");
        assertThat(targetInvoice.getEstadoCobro()).isEqualTo("PAGO_PARCIAL");
        verify(facturaRepository).save(invoice);
        verify(facturaRepository).save(targetInvoice);
    }
}
