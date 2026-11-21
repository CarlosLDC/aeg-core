package com.aeg.core.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.commercial.dto.CommercialPaymentRequest;
import com.aeg.core.company.Company;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;

class CommercialPaymentServiceImplTest {

    private PagoComercialRepository pagoRepository;
    private FacturaComercialRepository facturaRepository;
    private PrinterRepository printerRepository;
    private CommercialPaymentServiceImpl service;
    private FacturaComercial invoice;

    @BeforeEach
    void setUp() {
        pagoRepository = mock(PagoComercialRepository.class);
        facturaRepository = mock(FacturaComercialRepository.class);
        printerRepository = mock(PrinterRepository.class);
        service = new CommercialPaymentServiceImpl(pagoRepository, facturaRepository, printerRepository);

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
}
