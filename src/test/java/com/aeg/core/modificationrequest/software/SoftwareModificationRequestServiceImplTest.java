package com.aeg.core.modificationrequest.software;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequest;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationCreateRequest;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestDetailResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.software.Software;
import com.aeg.core.software.SoftwareRepository;
import com.aeg.core.software.SoftwareService;
import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;

class SoftwareModificationRequestServiceImplTest {

    private ModificationRequestRepository repository;
    private SoftwareRepository softwareRepository;
    private SoftwareService softwareService;
    private PrinterRepository printerRepository;
    private CompanyRepository companyRepository;
    private SecurityScopeService securityScope;
    private SoftwareModificationRequestServiceImpl service;

    private User currentUser;
    private Printer printer;

    @BeforeEach
    void setup() {
        repository = mock(ModificationRequestRepository.class);
        softwareRepository = mock(SoftwareRepository.class);
        softwareService = mock(SoftwareService.class);
        printerRepository = mock(PrinterRepository.class);
        companyRepository = mock(CompanyRepository.class);
        securityScope = mock(SecurityScopeService.class);

        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setName("Distribuidor User");
        currentUser.setRole(Role.DISTRIBUTOR);
        when(securityScope.currentUser()).thenReturn(currentUser);

        printer = new Printer();
        printer.setId(50L);
        printer.setFiscalSerial("ABC1234567");
        when(printerRepository.findById(50L)).thenReturn(Optional.of(printer));

        service = new SoftwareModificationRequestServiceImpl(
                repository,
                softwareRepository,
                softwareService,
                printerRepository,
                companyRepository,
                securityScope);
    }

    @Test
    void createRequest_savesPendingSoftwareRequest() {
        SoftwareModificationCreateRequest request = new SoftwareModificationCreateRequest(
                50L,
                "Sistema POS",
                "1.0.0",
                List.of("Java"),
                List.of("Linux"),
                "Dev Corp",
                "J-12345678-9",
                true,
                List.of(1L));

        when(softwareRepository.existsByNameIgnoreCaseAndVersionIgnoreCase("Sistema POS", "1.0.0")).thenReturn(false);
        when(repository.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.SOFTWARE, 50L, ModificationRequestStatus.PENDING)).thenReturn(Optional.empty());

        when(repository.save(any(ModificationRequest.class))).thenAnswer(inv -> {
            ModificationRequest mr = inv.getArgument(0);
            mr.setId(100L);
            return mr;
        });

        SoftwareModificationRequestDetailResponse response = service.createRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.printerId()).isEqualTo(50L);
        assertThat(response.name()).isEqualTo("Sistema POS");
        assertThat(response.version()).isEqualTo("1.0.0");
        assertThat(response.status()).isEqualTo(ModificationRequestStatus.PENDING);

        ArgumentCaptor<ModificationRequest> captor = ArgumentCaptor.forClass(ModificationRequest.class);
        verify(repository).save(captor.capture());
        ModificationRequest saved = captor.getValue();
        assertThat(saved.getTargetType()).isEqualTo(ModificationTargetType.SOFTWARE);
        assertThat(saved.getTargetId()).isEqualTo(50L);
        assertThat(saved.getActionType()).isEqualTo(ModificationActionType.CREATE);
        assertThat(saved.getRequestedBy()).isEqualTo(currentUser);
        assertThat(saved.getStatus()).isEqualTo(ModificationRequestStatus.PENDING);
    }

    @Test
    void createRequest_failsWhenSoftwareAlreadyExists() {
        SoftwareModificationCreateRequest request = new SoftwareModificationCreateRequest(
                50L, "Existing SW", "1.0.0", null, null, null, null, null, null);

        when(softwareRepository.existsByNameIgnoreCaseAndVersionIgnoreCase("Existing SW", "1.0.0")).thenReturn(true);

        assertThatThrownBy(() -> service.createRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El software ya existe");
    }

    @Test
    void approve_createsSoftwareAndAssignsToPrinter() {
        ModificationRequest mr = new ModificationRequest();
        mr.setId(101L);
        mr.setTargetType(ModificationTargetType.SOFTWARE);
        mr.setTargetId(50L);
        mr.setActionType(ModificationActionType.CREATE);
        mr.setStatus(ModificationRequestStatus.PENDING);
        mr.setProposedData(Map.of(
                "name", "FacturaPlus",
                "version", "2.0.0",
                "external", true,
                "developerName", "Soft SA",
                "developerRif", "J-99999999-9",
                "programmingLanguages", List.of("C#"),
                "operatingSystems", List.of("Windows"),
                "distributorCompanyIds", List.of(2L)));
        mr.setRequestedBy(currentUser);

        when(repository.findById(101L)).thenReturn(Optional.of(mr));
        when(softwareRepository.findByNameIgnoreCaseAndVersionIgnoreCase("FacturaPlus", "2.0.0")).thenReturn(Optional.empty());

        Software createdSoftware = new Software();
        createdSoftware.setId(33L);
        createdSoftware.setName("FacturaPlus");
        createdSoftware.setVersion("2.0.0");

        SoftwareResponse swResponse = new SoftwareResponse(
                33L, "FacturaPlus", "2.0.0", null, List.of("C#"), List.of("Windows"),
                "J-99999999-9", "Soft SA", true, List.of());
        when(softwareService.create(any(SoftwareRequest.class))).thenReturn(swResponse);
        when(softwareRepository.findById(33L)).thenReturn(Optional.of(createdSoftware));
        when(repository.save(any(ModificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SoftwareModificationRequestDetailResponse response = service.approve(101L);

        assertThat(response.status()).isEqualTo(ModificationRequestStatus.APPROVED);
        assertThat(printer.getSoftware()).isEqualTo(createdSoftware);
        verify(printerRepository).save(printer);
        verify(repository).save(mr);
    }

    @Test
    void reject_marksRequestAsRejected() {
        ModificationRequest mr = new ModificationRequest();
        mr.setId(102L);
        mr.setTargetType(ModificationTargetType.SOFTWARE);
        mr.setTargetId(50L);
        mr.setStatus(ModificationRequestStatus.PENDING);
        mr.setRequestedBy(currentUser);

        when(repository.findById(102L)).thenReturn(Optional.of(mr));
        when(repository.save(any(ModificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SoftwareModificationRequestDetailResponse response = service.reject(102L);

        assertThat(response.status()).isEqualTo(ModificationRequestStatus.REJECTED);
        verify(repository).save(mr);
        assertThat(printer.getSoftware()).isNull();
    }
}
