package com.aeg.core.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;
import com.aeg.core.printer.PrinterRepository;

class CommercialInvoiceServiceImplTest {

    private FacturaComercialRepository facturaRepository;
    private ClienteComercialRepository clienteRepository;
    private PagoComercialRepository pagoRepository;
    private PrinterRepository printerRepository;
    private CommercialInvoiceServiceImpl service;

    private ClienteComercial cliente;

    @BeforeEach
    void setUp() {
        facturaRepository = mock(FacturaComercialRepository.class);
        clienteRepository = mock(ClienteComercialRepository.class);
        pagoRepository = mock(PagoComercialRepository.class);
        printerRepository = mock(PrinterRepository.class);

        cliente = new ClienteComercial();
        cliente.setId(10L);
        cliente.setNombre("Inversiones Ejemplo");

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(facturaRepository.save(any(FacturaComercial.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new CommercialInvoiceServiceImpl(
                facturaRepository,
                clienteRepository,
                pagoRepository,
                printerRepository);
    }

    @Test
    void create_withFourRequiredFields_succeeds() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .clienteComercialId(10L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        CommercialInvoiceResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getNumeroFactura()).isEqualTo("FAC-001");
        assertThat(response.getClienteComercialId()).isEqualTo(10L);
        assertThat(response.getClienteComercialNombre()).isEqualTo("Inversiones Ejemplo");
        assertThat(response.getModalidad()).isEqualTo("VENTA");
        assertThat(response.getEstadoCobro()).isEqualTo("PAGADO");
        verify(facturaRepository).save(any(FacturaComercial.class));
    }

    @Test
    void create_withoutNumeroFactura_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("   ")
                .clienteComercialId(10L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("número de factura es obligatorio");
    }

    @Test
    void create_withoutCliente_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .clienteComercialId(null)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cliente es obligatorio");
    }
}
