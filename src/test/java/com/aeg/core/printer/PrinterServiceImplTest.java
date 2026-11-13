package com.aeg.core.printer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.aeg.core.client.ClientRepository;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.firmware.FirmwareRepository;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.organization.OrganizationCapabilityService;
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
}
