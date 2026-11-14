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

import com.aeg.core.branch.Branch;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.commercial.dto.CommercialInvoiceResponse;
import com.aeg.core.company.Company;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.PrinterRepository;

class CommercialInvoiceServiceImplTest {

    private FacturaComercialRepository facturaRepository;
    private DistributorRepository distributorRepository;
    private ClientRepository clientRepository;
    private PagoComercialRepository pagoRepository;
    private PrinterRepository printerRepository;
    private CommercialInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        facturaRepository = mock(FacturaComercialRepository.class);
        distributorRepository = mock(DistributorRepository.class);
        clientRepository = mock(ClientRepository.class);
        pagoRepository = mock(PagoComercialRepository.class);
        printerRepository = mock(PrinterRepository.class);

        Company distCompany = new Company();
        distCompany.setBusinessName("Distribuidora Norte C.A.");
        Branch distBranch = new Branch();
        distBranch.setCompany(distCompany);
        Distributor distributor = new Distributor();
        distributor.setId(5L);
        distributor.setBranch(distBranch);

        Company clientCompany = new Company();
        clientCompany.setBusinessName("Inversiones Ejemplo C.A.");
        Branch clientBranch = new Branch();
        clientBranch.setCompany(clientCompany);
        Client client = new Client();
        client.setId(10L);
        client.setBranch(clientBranch);

        when(distributorRepository.findById(5L)).thenReturn(Optional.of(distributor));
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(facturaRepository.save(any(FacturaComercial.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new CommercialInvoiceServiceImpl(
                facturaRepository,
                distributorRepository,
                clientRepository,
                pagoRepository,
                printerRepository);
    }

    @Test
    void create_withDistributor_succeeds() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .distribuidorId(5L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        CommercialInvoiceResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getNumeroFactura()).isEqualTo("FAC-001");
        assertThat(response.getDistribuidorId()).isEqualTo(5L);
        assertThat(response.getDistribuidorNombre()).isEqualTo("Distribuidora Norte C.A.");
        assertThat(response.getClienteId()).isNull();
        assertThat(response.getClienteComercialNombre()).isEqualTo("Distribuidora Norte C.A.");
        assertThat(response.getModalidad()).isEqualTo("VENTA");
        assertThat(response.getEstadoCobro()).isEqualTo("PAGADO");
        verify(facturaRepository).save(any(FacturaComercial.class));
    }

    @Test
    void create_withClient_succeeds() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-002")
                .clienteId(10L)
                .modalidad("CONSIGNACION")
                .estadoCobro("PENDIENTE_PAGO")
                .build();

        CommercialInvoiceResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getNumeroFactura()).isEqualTo("FAC-002");
        assertThat(response.getClienteId()).isEqualTo(10L);
        assertThat(response.getClienteNombre()).isEqualTo("Inversiones Ejemplo C.A.");
        assertThat(response.getDistribuidorId()).isNull();
        assertThat(response.getClienteComercialNombre()).isEqualTo("Inversiones Ejemplo C.A.");
        verify(facturaRepository).save(any(FacturaComercial.class));
    }

    @Test
    void create_withoutNumeroFactura_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("   ")
                .distribuidorId(5L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("número de factura es obligatorio");
    }

    @Test
    void create_withoutDistributorOrClient_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("distribuidor o un cliente");
    }

    @Test
    void create_withBothDistributorAndClient_throwsResponseStatusException() {
        CommercialInvoiceRequest request = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-001")
                .distribuidorId(5L)
                .clienteId(10L)
                .modalidad("VENTA")
                .estadoCobro("PAGADO")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no ambos");
    }
}
