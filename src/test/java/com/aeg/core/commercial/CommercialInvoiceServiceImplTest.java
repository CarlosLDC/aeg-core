package com.aeg.core.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.servicecenter.ResourceNotFoundException;

class CommercialInvoiceServiceImplTest {

    private FacturaComercialRepository facturaRepository;
    private CompanyRepository companyRepository;
    private PagoComercialRepository pagoRepository;
    private PrinterRepository printerRepository;
    private CommercialInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        facturaRepository = mock(FacturaComercialRepository.class);
        companyRepository = mock(CompanyRepository.class);
        pagoRepository = mock(PagoComercialRepository.class);
        printerRepository = mock(PrinterRepository.class);

        Company company = new Company();
        company.setId(7L);
        company.setBusinessName("Inversiones Ejemplo C.A.");
        company.setRif("J-12345678-9");

        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(facturaRepository.save(any(FacturaComercial.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new CommercialInvoiceServiceImpl(
                facturaRepository,
                companyRepository,
                pagoRepository,
                printerRepository);
    }

    @Test
    void create_withFourRequiredFields_succeeds() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        CommercialInvoiceResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getNumeroFactura()).isEqualTo("FAC-001");
        assertThat(response.getEmpresaId()).isEqualTo(7L);
        assertThat(response.getEmpresaNombre()).isEqualTo("Inversiones Ejemplo C.A.");
        assertThat(response.getEmpresaRif()).isEqualTo("J-123456789");
        assertThat(response.getClienteComercialNombre()).isEqualTo("Inversiones Ejemplo C.A.");
        assertThat(response.getModalidad()).isEqualTo("VENTA");
        assertThat(response.getEstadoCobro()).isEqualTo("PAGADO");
        verify(facturaRepository).save(any(FacturaComercial.class));
    }

    @Test
    void create_withoutNumeroFactura_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("   ")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("número de factura es obligatorio");
    }

    @Test
    void create_withoutEmpresa_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .empresaId(null)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("empresa es obligatoria");
    }

    @Test
    void syncPrinters_clearsPreviousAndAssignsDistinctIds() {
        FacturaComercial invoice = new FacturaComercial();
        invoice.setNumeroFactura("FAC-009");
        invoice.setModalidad("VENTA");
        invoice.setEstadoCobro("PAGADO");

        Printer p1 = new Printer();
        p1.setId(10L);
        Printer p2 = new Printer();
        p2.setId(11L);

        when(facturaRepository.findById(5L)).thenReturn(Optional.of(invoice));
        when(printerRepository.findAllById(Set.of(10L, 11L))).thenReturn(List.of(p1, p2));
        when(pagoRepository.countByFacturaComercial_Id(any())).thenReturn(0L);
        when(pagoRepository.findByFacturaComercial_Id(any())).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(any())).thenReturn(2L);

        CommercialInvoiceResponse response = service.syncPrinters(5L, List.of(10L, 11L, 10L));

        assertThat(response.getPrinterCount()).isEqualTo(2L);
        verify(printerRepository).clearFacturaComercialByFacturaId(invoice.getId());
        verify(printerRepository).assignFacturaComercialByPrinterIds(invoice, Set.of(10L, 11L));
    }

    @Test
    void syncPrinters_withMissingPrinterId_throwsResourceNotFound() {
        FacturaComercial invoice = new FacturaComercial();
        invoice.setNumeroFactura("FAC-009");

        when(facturaRepository.findById(5L)).thenReturn(Optional.of(invoice));
        when(printerRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.syncPrinters(5L, List.of(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(printerRepository, never()).clearFacturaComercialByFacturaId(any());
        verify(printerRepository, never()).assignFacturaComercialByPrinterIds(any(), any());
    }

    @Test
    void unlinkPrinter_clearsRelationForFacturaAndPrinter() {
        FacturaComercial invoice = new FacturaComercial();
        invoice.setNumeroFactura("FAC-009");

        when(facturaRepository.findById(5L)).thenReturn(Optional.of(invoice));
        when(pagoRepository.countByFacturaComercial_Id(any())).thenReturn(0L);
        when(pagoRepository.findByFacturaComercial_Id(any())).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(any())).thenReturn(1L);

        CommercialInvoiceResponse response = service.unlinkPrinter(5L, 10L);

        assertThat(response.getPrinterCount()).isEqualTo(1L);
        verify(printerRepository).clearFacturaComercialByFacturaIdAndPrinterId(invoice.getId(), 10L);
    }

    @Test
    void update_toPagado_transitionsLinkedCreditPrintersToEnajenada() {
        FacturaComercial invoice = invoiceWithId(5L, "PENDIENTE_PAGO");
        Printer creditPrinter = new Printer();
        creditPrinter.setId(21L);
        creditPrinter.setStatus(PrinterStatus.A_CREDITO);
        creditPrinter.setPaid(false);

        when(facturaRepository.findById(5L)).thenReturn(Optional.of(invoice));
        when(printerRepository.findByFacturaComercial_IdAndStatus(5L, PrinterStatus.A_CREDITO))
                .thenReturn(List.of(creditPrinter));
        when(pagoRepository.countByFacturaComercial_Id(5L)).thenReturn(0L);
        when(pagoRepository.findByFacturaComercial_Id(5L)).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(5L)).thenReturn(1L);

        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-005")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        CommercialInvoiceResponse response = service.update(5L, request);

        assertThat(response.getEstadoCobro()).isEqualTo("PAGADO");
        assertThat(creditPrinter.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(creditPrinter.getPaid()).isTrue();
        verify(printerRepository).saveAll(List.of(creditPrinter));
    }

    @Test
    void update_toNonPagado_doesNotSettlePrinters() {
        FacturaComercial invoice = invoiceWithId(5L, "PENDIENTE_PAGO");

        when(facturaRepository.findById(5L)).thenReturn(Optional.of(invoice));
        when(pagoRepository.countByFacturaComercial_Id(5L)).thenReturn(0L);
        when(pagoRepository.findByFacturaComercial_Id(5L)).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(5L)).thenReturn(1L);

        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-005")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PAGO_PARCIAL")
                .build();

        service.update(5L, request);

        verify(printerRepository, never()).findByFacturaComercial_IdAndStatus(any(), any());
        verify(printerRepository, never()).saveAll(any());
    }

    @Test
    void markAsPaid_setsEstadoCobroPagadoAndSettlesCreditPrinters() {
        FacturaComercial invoice = invoiceWithId(8L, "PAGO_PARCIAL");
        Printer creditPrinter = new Printer();
        creditPrinter.setId(31L);
        creditPrinter.setStatus(PrinterStatus.A_CREDITO);
        creditPrinter.setPaid(false);

        when(facturaRepository.findById(8L)).thenReturn(Optional.of(invoice));
        when(printerRepository.findByFacturaComercial_IdAndStatus(8L, PrinterStatus.A_CREDITO))
                .thenReturn(List.of(creditPrinter));
        when(pagoRepository.countByFacturaComercial_Id(8L)).thenReturn(1L);
        when(pagoRepository.findByFacturaComercial_Id(8L)).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(8L)).thenReturn(1L);

        CommercialInvoiceResponse response = service.markAsPaid(8L);

        assertThat(response.getEstadoCobro()).isEqualTo("PAGADO");
        assertThat(creditPrinter.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(creditPrinter.getPaid()).isTrue();
        verify(printerRepository).saveAll(List.of(creditPrinter));
    }

    @Test
    void syncPrinters_onPagadoInvoice_settlesNewlyLinkedCreditPrinters() {
        FacturaComercial invoice = invoiceWithId(9L, "PAGADO");
        Printer creditPrinter = new Printer();
        creditPrinter.setId(40L);
        creditPrinter.setStatus(PrinterStatus.A_CREDITO);
        creditPrinter.setPaid(false);

        when(facturaRepository.findById(9L)).thenReturn(Optional.of(invoice));
        when(printerRepository.findAllById(Set.of(40L))).thenReturn(List.of(creditPrinter));
        when(printerRepository.findByFacturaComercial_IdAndStatus(9L, PrinterStatus.A_CREDITO))
                .thenReturn(List.of(creditPrinter));
        when(pagoRepository.countByFacturaComercial_Id(9L)).thenReturn(0L);
        when(pagoRepository.findByFacturaComercial_Id(9L)).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(9L)).thenReturn(1L);

        service.syncPrinters(9L, List.of(40L));

        assertThat(creditPrinter.getStatus()).isEqualTo(PrinterStatus.ENAJENADA);
        assertThat(creditPrinter.getPaid()).isTrue();
        verify(printerRepository).saveAll(List.of(creditPrinter));
    }

    @Test
    void create_andFindById_computesMontoTotalAndRemainingBalance() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-MNT-1")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PAGO_PARCIAL")
                .montoTotal(new BigDecimal("900.00"))
                .build();

        CommercialInvoiceResponse created = service.create(request);
        assertThat(created.getMontoTotal()).isEqualByComparingTo("900.00");
        assertThat(created.getRemainingBalance()).isEqualByComparingTo("900.00");

        FacturaComercial savedEntity = invoiceWithId(12L, "PAGO_PARCIAL");
        savedEntity.setMontoTotal(new BigDecimal("900.00"));
        PagoComercial payment = new PagoComercial();
        payment.setMonto(new BigDecimal("350.00"));
        when(facturaRepository.findById(12L)).thenReturn(Optional.of(savedEntity));
        when(pagoRepository.countByFacturaComercial_Id(12L)).thenReturn(1L);
        when(pagoRepository.findByFacturaComercial_Id(12L)).thenReturn(List.of(payment));
        when(printerRepository.countByFacturaComercial_Id(12L)).thenReturn(2L);

        CommercialInvoiceResponse found = service.findById(12L);
        assertThat(found.getMontoTotal()).isEqualByComparingTo("900.00");
        assertThat(found.getPaidAmountTotal()).isEqualByComparingTo("350.00");
        assertThat(found.getRemainingBalance()).isEqualByComparingTo("550.00");
    }

    @Test
    void createAndUpdate_rejectDuplicateNumeroFacturaWithConflict() {
        when(facturaRepository.existsByNumeroFacturaIgnoreCase("FAC-DUP")).thenReturn(true);

        CommercialInvoiceRequest createReq = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-DUP")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(createReq))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        FacturaComercial existing = invoiceWithId(5L, "PENDIENTE_PAGO");
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(facturaRepository.existsByNumeroFacturaIgnoreCaseAndIdNot("FAC-DUP", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, createReq))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deleteAndUnlink_rejectProtectedCreditOrAuthorizedPrintersWithConflict() {
        FacturaComercial invoice = invoiceWithId(15L, "PENDIENTE_PAGO");
        when(facturaRepository.findById(15L)).thenReturn(Optional.of(invoice));

        Printer creditPrinter = new Printer();
        creditPrinter.setId(50L);
        creditPrinter.setFiscalSerial("GRA0000050");
        creditPrinter.setStatus(PrinterStatus.A_CREDITO);
        creditPrinter.setFacturaComercial(invoice);

        Printer authorizedConsignment = new Printer();
        authorizedConsignment.setId(51L);
        authorizedConsignment.setFiscalSerial("GRA0000051");
        authorizedConsignment.setStatus(PrinterStatus.EN_CONSIGNACION);
        authorizedConsignment.setEnajenacionAutorizada(true);
        authorizedConsignment.setFacturaComercial(invoice);

        when(printerRepository.findByFacturaComercial_IdOrderByFiscalSerialAsc(15L))
                .thenReturn(List.of(creditPrinter));
        when(printerRepository.findById(50L)).thenReturn(Optional.of(creditPrinter));
        when(printerRepository.findById(51L)).thenReturn(Optional.of(authorizedConsignment));

        assertThatThrownBy(() -> service.delete(15L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(facturaRepository, never()).delete(any());

        assertThatThrownBy(() -> service.unlinkPrinter(15L, 50L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.unlinkPrinter(15L, 51L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.syncPrinters(15L, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void syncPrinters_allowsReassigningProtectedPrinterFromAnotherInvoice() {
        FacturaComercial targetInvoice = invoiceWithId(20L, "PENDIENTE_PAGO");
        FacturaComercial sourceInvoice = invoiceWithId(21L, "PENDIENTE_PAGO");
        Printer protectedPrinter = new Printer();
        protectedPrinter.setId(60L);
        protectedPrinter.setFiscalSerial("GRA0000060");
        protectedPrinter.setStatus(PrinterStatus.A_CREDITO);
        protectedPrinter.setFacturaComercial(sourceInvoice);

        when(facturaRepository.findById(20L)).thenReturn(Optional.of(targetInvoice));
        when(printerRepository.findAllById(Set.of(60L))).thenReturn(List.of(protectedPrinter));
        when(printerRepository.findByFacturaComercial_IdOrderByFiscalSerialAsc(20L)).thenReturn(List.of());
        when(pagoRepository.countByFacturaComercial_Id(20L)).thenReturn(0L);
        when(pagoRepository.findByFacturaComercial_Id(20L)).thenReturn(List.of());
        when(printerRepository.countByFacturaComercial_Id(20L)).thenReturn(1L);

        CommercialInvoiceResponse res = service.syncPrinters(20L, List.of(60L));

        assertThat(res.getPrinterCount()).isEqualTo(1L);
        verify(printerRepository).assignFacturaComercialByPrinterIds(targetInvoice, Set.of(60L));
    }

    private static FacturaComercial invoiceWithId(Long id, String estadoCobro) {
        FacturaComercial invoice = new FacturaComercial();
        try {
            var idField = FacturaComercial.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoice, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        invoice.setNumeroFactura("FAC-" + id);
        invoice.setModalidad("VENTA");
        invoice.setEstadoCobro(estadoCobro);
        return invoice;
    }
}
