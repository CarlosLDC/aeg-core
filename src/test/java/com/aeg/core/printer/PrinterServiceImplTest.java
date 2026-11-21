package com.aeg.core.printer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.aeg.core.client.ClientRepository;
import com.aeg.core.commercial.CommercialInvoiceService;
import com.aeg.core.commercial.FacturaComercial;
import com.aeg.core.commercial.FacturaComercialRepository;
import com.aeg.core.commercial.PagoComercialRepository;
import com.aeg.core.commercial.dto.CommercialInvoiceRequest;
import com.aeg.core.company.Company;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.firmware.FirmwareRepository;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.organization.OrganizationCapabilityService;
import com.aeg.core.printer.dto.PrinterEnajenacionAuthorizeRequest;
import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printer.dto.PrinterResponse;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.software.Software;
import com.aeg.core.software.SoftwareRepository;
import com.aeg.core.technicalservice.TechnicalServiceVisitRepository;

class PrinterServiceImplTest {

    private PrinterRepository repository;
    private PrinterModelRepository modelRepository;
    private SoftwareRepository softwareRepository;
    private FirmwareRepository firmwareRepository;
    private DistributorRepository distributorRepository;
    private ClientRepository clientRepository;
    private SealRepository sealRepository;
    private AnnualInspectionRepository annualInspectionRepository;
    private TechnicalServiceVisitRepository technicalServiceVisitRepository;
    private SecurityScopeService securityScope;
    private OrganizationCapabilityService organizationCapability;
    private PrinterServiceImpl service;

    private User distributorUser;
    private Printer printer;

    @BeforeEach
    void setup() {
        repository = mock(PrinterRepository.class);
        modelRepository = mock(PrinterModelRepository.class);
        softwareRepository = mock(SoftwareRepository.class);
        firmwareRepository = mock(FirmwareRepository.class);
        distributorRepository = mock(DistributorRepository.class);
        clientRepository = mock(ClientRepository.class);
        sealRepository = mock(SealRepository.class);
        annualInspectionRepository = mock(AnnualInspectionRepository.class);
        technicalServiceVisitRepository = mock(TechnicalServiceVisitRepository.class);
        securityScope = mock(SecurityScopeService.class);
        organizationCapability = mock(OrganizationCapabilityService.class);

        distributorUser = new User();
        distributorUser.setId(10L);
        distributorUser.setRole(Role.DISTRIBUTOR);
        distributorUser.setDistributorId(5L);
        when(securityScope.currentUser()).thenReturn(distributorUser);

        printer = new Printer();
        printer.setId(1L);
        printer.setFiscalSerial("ABC1234567");
        printer.setStatus(PrinterStatus.ASIGNADA);
        when(repository.findById(1L)).thenReturn(Optional.of(printer));
        when(repository.save(any(Printer.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new PrinterServiceImpl(
                repository,
                modelRepository,
                softwareRepository,
                firmwareRepository,
                distributorRepository,
                clientRepository,
                sealRepository,
                annualInspectionRepository,
                technicalServiceVisitRepository,
                securityScope,
                organizationCapability);
    }

    @Test
    void update_distributorThrowsAccessDenied() {
        PrinterRequest request = new PrinterRequest(
                1L, 77L, null, 5L, "ABC1234567", null, true, null, null, null,
                PrinterStatus.ASIGNADA, null, null);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Distributors cannot edit printer records");
    }

    @Test
    void updateSoftware_distributorCanAssignExistingSoftware() {
        Software software = new Software();
        software.setId(77L);
        software.setName("Sistema Contable");
        software.setVersion("1.0.0");
        when(softwareRepository.findById(77L)).thenReturn(Optional.of(software));

        PrinterResponse response = service.updateSoftware(1L, 77L);

        assertThat(response).isNotNull();
        assertThat(printer.getSoftware()).isEqualTo(software);
        verify(repository).save(printer);
    }

    @Test
    void updateSoftware_distributorCanClearSoftware() {
        Software software = new Software();
        software.setId(77L);
        printer.setSoftware(software);

        PrinterResponse response = service.updateSoftware(1L, null);

        assertThat(response).isNotNull();
        assertThat(printer.getSoftware()).isNull();
        verify(repository).save(printer);
    }

    @Test
    void create_boxWith8Printers_throwsIllegalArgumentException() {
        com.aeg.core.inventorybox.InventoryBoxRepository boxRepo = mock(com.aeg.core.inventorybox.InventoryBoxRepository.class);
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        when(securityScope.currentUser()).thenReturn(adminUser);

        com.aeg.core.printermodel.PrinterModel model = new com.aeg.core.printermodel.PrinterModel();
        model.setId(1L);
        model.setSerialPrefix("ABC");
        when(modelRepository.findById(1L)).thenReturn(Optional.of(model));

        when(repository.existsByFiscalSerialIgnoreCase("ABC1234567")).thenReturn(false);
        when(repository.countByBox_Id(10L)).thenReturn(8L);

        PrinterServiceImpl adminService = new PrinterServiceImpl(
                repository,
                modelRepository,
                softwareRepository,
                firmwareRepository,
                distributorRepository,
                clientRepository,
                sealRepository,
                annualInspectionRepository,
                technicalServiceVisitRepository,
                securityScope,
                organizationCapability,
                null,
                null,
                boxRepo);

        PrinterRequest request = new PrinterRequest(
                1L, null, null, null, "ABC1234567", null, false, null, null, null,
                null, PrinterStatus.SIN_ASIGNAR, null, null, 10L);

        assertThatThrownBy(() -> adminService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene el límite máximo de 8 impresoras");
    }

    @Test
    void authorizeEnajenacion_withExistingInvoice_linksWithoutMutatingInvoice() throws Exception {
        User adminUser = adminUser();
        when(securityScope.currentUser()).thenReturn(adminUser);

        printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        printer.setEnajenacionAutorizada(false);

        FacturaComercial existing = invoiceWithId(44L, "FAC-044", "CONSIGNACION", "PENDIENTE_PAGO");
        FacturaComercialRepository facturaRepo = mock(FacturaComercialRepository.class);
        CommercialInvoiceService invoiceService = mock(CommercialInvoiceService.class);
        PagoComercialRepository pagoRepo = mock(PagoComercialRepository.class);
        when(facturaRepo.findById(44L)).thenReturn(Optional.of(existing));

        PrinterServiceImpl adminService = adminPrinterService(facturaRepo, invoiceService, pagoRepo);

        PrinterResponse response = adminService.authorizeEnajenacion(
                1L,
                new PrinterEnajenacionAuthorizeRequest(4, new BigDecimal("480.00"), 44L, null));

        assertThat(printer.getEnajenacionAutorizada()).isTrue();
        assertThat(printer.getFacturaComercial()).isSameAs(existing);
        assertThat(existing.getModalidad()).isEqualTo("CONSIGNACION");
        assertThat(existing.getEstadoCobro()).isEqualTo("PENDIENTE_PAGO");
        assertThat(response.facturaComercialId()).isEqualTo(44L);
        assertThat(response.facturaComercialNumero()).isEqualTo("FAC-044");
        assertThat(response.facturaComercialModalidad()).isEqualTo("CONSIGNACION");
        assertThat(response.facturaComercialEstadoCobro()).isEqualTo("PENDIENTE_PAGO");
        verify(invoiceService, never()).createEntity(any());
    }

    @Test
    void authorizeEnajenacion_withNewCommercialInvoice_createsAndLinksInvoice() throws Exception {
        User adminUser = adminUser();
        when(securityScope.currentUser()).thenReturn(adminUser);

        printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        printer.setEnajenacionAutorizada(false);

        CommercialInvoiceRequest newInvReq = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-900")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PENDIENTE_PAGO")
                .build();
        FacturaComercial created = invoiceWithId(90L, "FAC-900", "VENTA", "PENDIENTE_PAGO");

        FacturaComercialRepository facturaRepo = mock(FacturaComercialRepository.class);
        CommercialInvoiceService invoiceService = mock(CommercialInvoiceService.class);
        PagoComercialRepository pagoRepo = mock(PagoComercialRepository.class);
        when(invoiceService.createEntity(newInvReq)).thenReturn(created);

        PrinterServiceImpl adminService = adminPrinterService(facturaRepo, invoiceService, pagoRepo);

        PrinterResponse response = adminService.authorizeEnajenacion(
                1L,
                new PrinterEnajenacionAuthorizeRequest(6, new BigDecimal("600.00"), null, newInvReq));

        assertThat(printer.getFacturaComercial()).isSameAs(created);
        assertThat(response.facturaComercialId()).isEqualTo(90L);
        assertThat(response.facturaComercialNumero()).isEqualTo("FAC-900");
        verify(invoiceService).createEntity(newInvReq);
    }

    @Test
    void authorizeEnajenacion_rejectsMissingInvoice() {
        when(securityScope.currentUser()).thenReturn(adminUser());
        printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        printer.setEnajenacionAutorizada(false);

        PrinterServiceImpl adminService = adminPrinterService(
                mock(FacturaComercialRepository.class),
                mock(CommercialInvoiceService.class),
                mock(PagoComercialRepository.class));

        assertThatThrownBy(() -> adminService.authorizeEnajenacion(
                1L,
                new PrinterEnajenacionAuthorizeRequest(3, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("ya "));
    }

    @Test
    void authorizeEnajenacion_rejectsBothExistingAndNewInvoice() {
        when(securityScope.currentUser()).thenReturn(adminUser());
        printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        printer.setEnajenacionAutorizada(false);

        PrinterServiceImpl adminService = adminPrinterService(
                mock(FacturaComercialRepository.class),
                mock(CommercialInvoiceService.class),
                mock(PagoComercialRepository.class));

        CommercialInvoiceRequest newInvReq = CommercialInvoiceRequest.builder()
                .numeroFactura("FAC-901")
                .empresaId(7L)
                .modalidad("VENTA")
                .estadoCobro("PENDIENTE_PAGO")
                .build();

        assertThatThrownBy(() -> adminService.authorizeEnajenacion(
                1L,
                new PrinterEnajenacionAuthorizeRequest(3, null, 44L, newInvReq)))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("ya "));
    }

    @Test
    void authorizeEnajenacion_rejectsNonAdmin() {
        when(securityScope.currentUser()).thenReturn(distributorUser);
        PrinterServiceImpl distService = adminPrinterService(
                mock(FacturaComercialRepository.class),
                mock(CommercialInvoiceService.class),
                mock(PagoComercialRepository.class));

        assertThatThrownBy(() -> distService.authorizeEnajenacion(
                1L,
                new PrinterEnajenacionAuthorizeRequest(3, null, 44L, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void authorizeEnajenacion_rejectsNonConsignmentPrinter() {
        when(securityScope.currentUser()).thenReturn(adminUser());
        printer.setStatus(PrinterStatus.ASIGNADA);

        PrinterServiceImpl adminService = adminPrinterService(
                mock(FacturaComercialRepository.class),
                mock(CommercialInvoiceService.class),
                mock(PagoComercialRepository.class));

        assertThatThrownBy(() -> adminService.authorizeEnajenacion(
                1L,
                new PrinterEnajenacionAuthorizeRequest(3, null, 44L, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PrinterServiceImpl adminPrinterService(
            FacturaComercialRepository facturaRepo,
            CommercialInvoiceService invoiceService,
            PagoComercialRepository pagoRepo) {
        return new PrinterServiceImpl(
                repository,
                modelRepository,
                softwareRepository,
                firmwareRepository,
                distributorRepository,
                clientRepository,
                sealRepository,
                annualInspectionRepository,
                technicalServiceVisitRepository,
                securityScope,
                organizationCapability,
                null,
                null,
                null,
                facturaRepo,
                invoiceService,
                pagoRepo);
    }

    private static User adminUser() {
        User admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setRole(Role.ADMIN);
        return admin;
    }

    private static FacturaComercial invoiceWithId(
            Long id,
            String numeroFactura,
            String modalidad,
            String estadoCobro) throws Exception {
        Company company = new Company();
        company.setId(7L);
        company.setBusinessName("Demo C.A.");
        FacturaComercial invoice = new FacturaComercial();
        var idField = FacturaComercial.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(invoice, id);
        invoice.setCompany(company);
        invoice.setNumeroFactura(numeroFactura);
        invoice.setModalidad(modalidad);
        invoice.setEstadoCobro(estadoCobro);
        return invoice;
    }
}
