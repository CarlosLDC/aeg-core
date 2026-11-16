package com.aeg.core.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
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
}
