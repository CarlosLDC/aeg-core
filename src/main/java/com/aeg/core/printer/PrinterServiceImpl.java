package com.aeg.core.printer;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.dto.PrinterDispositionRequest;
import com.aeg.core.printer.dto.PrinterEnajenacionTicketResponse;
import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printer.dto.PrinterResponse;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.branch.Branch;
import com.aeg.core.client.Client;
import com.aeg.core.company.Company;
import com.aeg.core.enajenacion.mqtt.EnajenacionTicketExtractor;
import com.aeg.core.organization.OrgCapability;
import com.aeg.core.organization.OrganizationCapabilityService;

@Service
@Transactional
public class PrinterServiceImpl implements PrinterService {

    private final PrinterRepository repository;
    private final com.aeg.core.printermodel.PrinterModelRepository modelRepository;
    private final com.aeg.core.software.SoftwareRepository softwareRepository;
    private final com.aeg.core.distributor.DistributorRepository distributorRepository;
    private final com.aeg.core.client.ClientRepository clientRepository;
    private final SecurityScopeService securityScope;
    private final OrganizationCapabilityService organizationCapability;

    public PrinterServiceImpl(PrinterRepository repository,
                              com.aeg.core.printermodel.PrinterModelRepository modelRepository,
                              com.aeg.core.software.SoftwareRepository softwareRepository,
                              com.aeg.core.distributor.DistributorRepository distributorRepository,
                              com.aeg.core.client.ClientRepository clientRepository,
                              SecurityScopeService securityScope,
                              OrganizationCapabilityService organizationCapability) {
        this.repository = repository;
        this.modelRepository = modelRepository;
        this.softwareRepository = softwareRepository;
        this.distributorRepository = distributorRepository;
        this.clientRepository = clientRepository;
        this.securityScope = securityScope;
        this.organizationCapability = organizationCapability;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrinterResponse> findAll() {
        return securityScope.findVisiblePrinters().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterResponse findById(Long id) {
        Printer printer = findEntityById(id);
        securityScope.assertPrinterInScope(printer);
        return toResponse(printer);
    }

    @Override
    public PrinterResponse create(PrinterRequest request) {
        if (repository.existsByFiscalSerialIgnoreCase(request.fiscalSerial())) {
            throw new IllegalArgumentException("fiscalSerial already exists: " + request.fiscalSerial());
        }
        Printer p = new Printer();
        if (request.modelId() != null) {
            p.setModel(modelRepository.findById(request.modelId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer model not found with id: " + request.modelId())));
        }
        if (request.softwareId() != null) {
            p.setSoftware(softwareRepository.findById(request.softwareId())
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with id: " + request.softwareId())));
        }
        assertPrinterAssignmentAllowed(p, null, request.distributorId());
        applyDistributor(p, resolveDistributorIdForWrite(request.distributorId()));
        if (request.clientId() != null) {
            var client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
            securityScope.assertClientInScope(client);
            p.setClient(client);
        } else {
            p.setClient(null);
        }
        p.setFiscalSerial(request.fiscalSerial());
        p.setFinalSalePrice(request.finalSalePrice());
        p.setPaid(request.paid());
        p.setInstallationDate(request.installationDate());
        p.setVersionFirmware(request.versionFirmware());
        p.setMacAddress(request.macAddress());
        p.setStatus(request.status());
        p.setDeviceType(request.deviceType());
        p.setCreationBatchId(request.creationBatchId());
        reconcileDistributorPaymentStatus(p);
        return toResponse(repository.save(p));
    }

    @Override
    public PrinterResponse update(Long id, PrinterRequest request) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        if (Role.isDistributorScoped(currentUser().getRole())) {
            return toResponse(applyDistributorDisposition(p, request));
        }
        if (!p.getFiscalSerial().equalsIgnoreCase(request.fiscalSerial()) && repository.existsByFiscalSerialIgnoreCase(request.fiscalSerial())) {
            throw new IllegalArgumentException("fiscalSerial already exists: " + request.fiscalSerial());
        }
        if (request.modelId() != null) {
            p.setModel(modelRepository.findById(request.modelId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer model not found with id: " + request.modelId())));
        }
        if (request.softwareId() != null) {
            p.setSoftware(softwareRepository.findById(request.softwareId())
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with id: " + request.softwareId())));
        }
        Long resolvedDistributorId = resolveDistributorIdForWrite(request.distributorId());
        Long previousDistributorId = p.getDistributorId();
        if (request.status() == PrinterStatus.SIN_ASIGNAR
                || request.status() == PrinterStatus.DE_FABRICA) {
            applyDistributor(p, null);
        } else if (resolvedDistributorId != null) {
            assertPrinterAssignmentAllowed(p, previousDistributorId, resolvedDistributorId);
            applyDistributor(p, resolvedDistributorId);
        }
        if (request.clientId() != null) {
            var client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
            securityScope.assertClientInScope(client);
            p.setClient(client);
        } else {
            if (p.getStatus() == PrinterStatus.ENAJENADA) {
                throw new IllegalArgumentException(
                        "No se puede quitar el cliente de una impresora enajenada.");
            }
            p.setClient(null);
        }
        p.setFiscalSerial(request.fiscalSerial());
        p.setFinalSalePrice(request.finalSalePrice());
        p.setPaid(request.paid());
        if (request.installationDate() != null) {
            p.setInstallationDate(request.installationDate());
        }
        p.setVersionFirmware(request.versionFirmware());
        p.setMacAddress(request.macAddress());
        p.setStatus(request.status());
        p.setDeviceType(request.deviceType());
        reconcileDistributorPaymentStatus(p);
        return toResponse(repository.save(p));
    }

    @Override
    public PrinterResponse dispose(Long id, PrinterDispositionRequest request) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        organizationCapability.assertActorCan(OrgCapability.ENAJENAR);
        return toResponse(applyDisposition(p, request));
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterEnajenacionTicketResponse previewEnajenacionTicket(Long printerId, Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId is required");
        }
        Printer printer = findEntityById(printerId);
        securityScope.assertPrinterInScope(printer);
        organizationCapability.assertActorCan(OrgCapability.ENAJENAR);
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        securityScope.assertClientInScope(client);
        Branch branch = client.getBranch();
        if (branch == null) {
            throw new IllegalArgumentException("Client branch is missing");
        }
        Company company = branch.getCompany();
        if (company == null) {
            throw new IllegalArgumentException("Client company is missing");
        }
        if (branch.getAddress() == null || branch.getAddress().isBlank()
                || branch.getCity() == null || branch.getCity().isBlank()
                || branch.getState() == null || branch.getState().isBlank()) {
            throw new IllegalArgumentException("Branch address, city and state are required");
        }
        return new PrinterEnajenacionTicketResponse(
                EnajenacionTicketExtractor.buildDefaultHeader(branch, company),
                EnajenacionTicketExtractor.buildDefaultTrailer());
    }

    @Override
    public void delete(Long id) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        repository.delete(p);
    }

    private Printer findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + id));
    }

    private User currentUser() {
        return securityScope.currentUser();
    }

    private Long resolveDistributorIdForWrite(Long requestedDistributorId) {
        User user = currentUser();
        if (!Role.isDistributorScoped(user.getRole())) {
            return requestedDistributorId;
        }
        Long ownDistributorId = user.getDistributorId();
        if (ownDistributorId == null) {
            throw new AccessDeniedException("Distributor user has no distributor assignment");
        }
        if (requestedDistributorId != null && !requestedDistributorId.equals(ownDistributorId)) {
            throw new AccessDeniedException("Cannot assign printer to another distributor");
        }
        return ownDistributorId;
    }

    private void applyDistributor(Printer printer, Long distributorId) {
        if (distributorId != null) {
            printer.setDistributor(distributorRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + distributorId)));
        } else if (!Role.isDistributorScoped(currentUser().getRole())) {
            printer.setDistributor(null);
        }
    }

    /**
     * Distribuidor: única mutación permitida — enajenar impresora asignada a un cliente propio.
     */
    private Printer applyDistributorDisposition(Printer printer, PrinterRequest request) {
        if (request.status() == PrinterStatus.ENAJENADA) {
            throw new IllegalArgumentException(
                    "Use POST /api/printers/{id}/enajenar with header and trailer to register enajenacion ticket");
        }
        throw new AccessDeniedException("Distributors can only dispose assigned printers to a client");
    }

    private void assertPrinterAssignmentAllowed(
            Printer printer,
            Long previousDistributorId,
            Long requestedDistributorId) {
        Long resolved = resolveDistributorIdForWrite(requestedDistributorId);
        if (resolved == null) {
            return;
        }
        if (previousDistributorId != null && previousDistributorId.equals(resolved)) {
            return;
        }
        PrinterStatus status = printer.getStatus();
        if (status == PrinterStatus.DE_FABRICA || status == PrinterStatus.SIN_ASIGNAR) {
            organizationCapability.assertActorCan(OrgCapability.ASSIGN_TO_DISTRIBUTOR);
        }
    }

    /**
     * Impresoras con distribuidor y sin pago quedan en consignación; al pagar pasan a asignada.
     */
    private void reconcileDistributorPaymentStatus(Printer printer) {
        if (printer.getDistributor() == null) {
            return;
        }
        PrinterStatus status = printer.getStatus();
        if (status == PrinterStatus.ENAJENADA
                || status == PrinterStatus.DESINCORPORADA
                || status == PrinterStatus.LABORATORIO
                || status == PrinterStatus.DE_FABRICA
                || status == PrinterStatus.SIN_ASIGNAR) {
            return;
        }
        if (Boolean.TRUE.equals(printer.getPaid())) {
            if (status == PrinterStatus.EN_CONSIGNACION) {
                printer.setStatus(PrinterStatus.ASIGNADA);
            }
        } else if (status == PrinterStatus.ASIGNADA || status == PrinterStatus.EN_CONSIGNACION) {
            printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        }
    }

    private Printer applyDisposition(Printer printer, PrinterDispositionRequest request) {
        if (printer.getStatus() != PrinterStatus.ASIGNADA) {
            throw new IllegalArgumentException("Only assigned printers can be disposed to a client");
        }
        if (!Boolean.TRUE.equals(printer.getPaid())) {
            throw new IllegalArgumentException(
                    "Solo se pueden enajenar impresoras con estatus de pago Pagada.");
        }
        if (request.clientId() == null) {
            throw new IllegalArgumentException("clientId is required to dispose a printer");
        }

        var client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
        securityScope.assertClientInScope(client);
        printer.setClient(client);
        printer.setHeader(PrinterTicketValidator.normalizeHeader(request.header()));
        printer.setTrailer(PrinterTicketValidator.normalizeTrailer(request.trailer()));
        printer.setInstallationDate(
                request.installationDate() != null
                        ? request.installationDate()
                        : java.time.OffsetDateTime.now());
        return repository.save(printer);
    }

    private PrinterResponse toResponse(Printer p) {
        return new PrinterResponse(
                p.getId(),
                p.getModelId(),
                p.getSoftwareId(),
                p.getClientId(),
                p.getFiscalSerial(),
                p.getFinalSalePrice(),
                p.getCreatedAt(),
                p.getCreationBatchId(),
                p.getStatus(),
                p.getDistributorId(),
                p.getPaid(),
                p.getInstallationDate(),
                p.getVersionFirmware(),
                p.getMacAddress(),
                p.getDeviceType(),
                p.getHeader(),
                p.getTrailer()
        );
    }
}
