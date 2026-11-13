package com.aeg.core.printer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.printer.credit.PrinterCreditPaymentRepository;

import com.aeg.core.printer.dto.PrinterDeleteImpactResponse;
import com.aeg.core.printer.dto.PrinterDependencyRef;
import com.aeg.core.printer.dto.PrinterDispositionRequest;
import com.aeg.core.printer.dto.PrinterEnajenacionAuthorizeRequest;
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
import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.inspection.AnnualInspection;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.organization.OrgCapability;
import com.aeg.core.organization.OrganizationCapabilityService;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;
import com.aeg.core.technicalservice.TechnicalServiceVisit;
import com.aeg.core.technicalservice.TechnicalServiceVisitRepository;

@Service
@Transactional
public class PrinterServiceImpl implements PrinterService {

    private final PrinterRepository repository;
    private final com.aeg.core.printermodel.PrinterModelRepository modelRepository;
    private final com.aeg.core.software.SoftwareRepository softwareRepository;
    private final com.aeg.core.firmware.FirmwareRepository firmwareRepository;
    private final com.aeg.core.distributor.DistributorRepository distributorRepository;
    private final com.aeg.core.client.ClientRepository clientRepository;
    private final SealRepository sealRepository;
    private final AnnualInspectionRepository annualInspectionRepository;
    private final TechnicalServiceVisitRepository technicalServiceVisitRepository;
    private final SecurityScopeService securityScope;
    private final OrganizationCapabilityService organizationCapability;
    private final ModificationRequestRepository modificationRequestRepository;
    private final PrinterCreditPaymentRepository creditPaymentRepository;
    /**
     * Si es true, el listado omite header/trailer (JSON pesado) y expone solo hasTicketConfig.
     * Poner printers.list.slim-ticket=false para revertir sin tocar código.
     */
    @org.springframework.beans.factory.annotation.Value("${printers.list.slim-ticket:true}")
    private boolean slimTicketInList;

    public PrinterServiceImpl(PrinterRepository repository,
                              com.aeg.core.printermodel.PrinterModelRepository modelRepository,
                              com.aeg.core.software.SoftwareRepository softwareRepository,
                              com.aeg.core.firmware.FirmwareRepository firmwareRepository,
                              com.aeg.core.distributor.DistributorRepository distributorRepository,
                              com.aeg.core.client.ClientRepository clientRepository,
                              SealRepository sealRepository,
                              AnnualInspectionRepository annualInspectionRepository,
                              TechnicalServiceVisitRepository technicalServiceVisitRepository,
                              SecurityScopeService securityScope,
                              OrganizationCapabilityService organizationCapability) {
        this(repository, modelRepository, softwareRepository, firmwareRepository, distributorRepository,
                clientRepository, sealRepository, annualInspectionRepository, technicalServiceVisitRepository,
                securityScope, organizationCapability, null, null);
    }

    @Autowired
    public PrinterServiceImpl(PrinterRepository repository,
                              com.aeg.core.printermodel.PrinterModelRepository modelRepository,
                              com.aeg.core.software.SoftwareRepository softwareRepository,
                              com.aeg.core.firmware.FirmwareRepository firmwareRepository,
                              com.aeg.core.distributor.DistributorRepository distributorRepository,
                              com.aeg.core.client.ClientRepository clientRepository,
                              SealRepository sealRepository,
                              AnnualInspectionRepository annualInspectionRepository,
                              TechnicalServiceVisitRepository technicalServiceVisitRepository,
                              SecurityScopeService securityScope,
                              OrganizationCapabilityService organizationCapability,
                              ModificationRequestRepository modificationRequestRepository,
                              PrinterCreditPaymentRepository creditPaymentRepository) {
        this.repository = repository;
        this.modelRepository = modelRepository;
        this.softwareRepository = softwareRepository;
        this.firmwareRepository = firmwareRepository;
        this.distributorRepository = distributorRepository;
        this.clientRepository = clientRepository;
        this.sealRepository = sealRepository;
        this.annualInspectionRepository = annualInspectionRepository;
        this.technicalServiceVisitRepository = technicalServiceVisitRepository;
        this.securityScope = securityScope;
        this.organizationCapability = organizationCapability;
        this.modificationRequestRepository = modificationRequestRepository;
        this.creditPaymentRepository = creditPaymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrinterResponse> findAll() {
        List<Printer> printers = securityScope.findVisiblePrinters();
        if (printers.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> pendingRequestsByPrinter = loadPendingEnajenacionRequestMap();
        Map<Long, CreditTotals> creditTotalsByPrinter = loadCreditTotalsMap(printers);
        return printers.stream()
                .map(p -> toResponse(
                        p,
                        !slimTicketInList,
                        pendingRequestsByPrinter.get(p.getId()),
                        creditTotalsByPrinter.get(p.getId())))
                .toList();
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
        String normalizedMac = normalizeMacOrNull(request.macAddress());
        assertMacAvailable(normalizedMac, null);
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
        String normalizedSerial = normalizeFiscalSerialForModel(request.fiscalSerial(), p.getModel());
        p.setFiscalSerial(normalizedSerial);
        p.setFinalSalePrice(request.finalSalePrice());
        p.setPaid(request.paid());
        p.setInstallationDate(request.installationDate());
        p.setFirmware(resolveFirmware(request.firmwareId(), request.versionFirmware(), request.modelId()));
        p.setMacAddress(normalizedMac);
        p.setStatus(request.status());
        p.setCreationBatchId(request.creationBatchId());
        String encryptionKey = request.encryptionKey();
        if ((encryptionKey == null || encryptionKey.isBlank()) && normalizedMac != null && request.fiscalSerial() != null) {
            encryptionKey = PrinterEncryptionKeyGenerator.generateKey(request.fiscalSerial(), normalizedMac);
        }
        p.setEncryptionKey(encryptionKey);
        reconcileDistributorPaymentStatus(p, null, request.paid());
        return toResponse(repository.save(p));
    }

    @Override
    public PrinterResponse update(Long id, PrinterRequest request) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        if (Role.isDistributorScoped(currentUser().getRole())) {
            throw new AccessDeniedException("Distributors cannot edit printer records. Use software assignment.");
        }
        if (!p.getFiscalSerial().equalsIgnoreCase(request.fiscalSerial()) && repository.existsByFiscalSerialIgnoreCase(request.fiscalSerial())) {
            throw new IllegalArgumentException("fiscalSerial already exists: " + request.fiscalSerial());
        }
        String normalizedMac = normalizeMacOrNull(request.macAddress());
        assertMacAvailable(normalizedMac, p.getId());
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
        PrinterStatus previousStatus = p.getStatus();
        if (request.status() == PrinterStatus.SIN_ASIGNAR
                || request.status() == PrinterStatus.DE_FABRICA
                || resolvedDistributorId == null) {
            applyDistributor(p, null);
        } else {
            assertPrinterAssignmentAllowed(p, previousDistributorId, resolvedDistributorId);
            applyDistributor(p, resolvedDistributorId);
        }
        if (request.clientId() != null && request.status() != PrinterStatus.SIN_ASIGNAR && request.status() != PrinterStatus.DE_FABRICA) {
            var client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
            securityScope.assertClientInScope(client);
            p.setClient(client);
        } else {
            p.setClient(null);
        }
        if (request.status() == PrinterStatus.SIN_ASIGNAR
                || request.status() == PrinterStatus.DE_FABRICA) {
            p.setHeader(null);
            p.setTrailer(null);
            p.setInstallationDate(null);
            p.setEnajenacionAutorizada(false);
            p.setEnajenacionAutorizadaAt(null);
            p.setEnajenacionAutorizadaBy(null);
            p.setCreditInstallments(null);
        } else if (!request.status().isDisposed()) {
            if (p.getStatus().isDisposed() || p.getClient() == null) {
                p.setHeader(null);
                p.setTrailer(null);
            }
            p.setInstallationDate(request.installationDate());
            if (request.status() != PrinterStatus.EN_CONSIGNACION) {
                p.setEnajenacionAutorizada(false);
                p.setEnajenacionAutorizadaAt(null);
                p.setEnajenacionAutorizadaBy(null);
            }
        } else if (request.installationDate() != null) {
            p.setInstallationDate(request.installationDate());
        }
        String normalizedSerial = normalizeFiscalSerialForModel(request.fiscalSerial(), p.getModel());
        p.setFiscalSerial(normalizedSerial);
        p.setFinalSalePrice(request.finalSalePrice());
        p.setPaid(request.paid());
        p.setFirmware(resolveFirmware(request.firmwareId(), request.versionFirmware(), p.getModelId()));
        boolean macChanged = !java.util.Objects.equals(p.getMacAddress(), normalizedMac);
        p.setMacAddress(normalizedMac);
        p.setStatus(request.status());
        if (request.encryptionKey() != null) {
            p.setEncryptionKey(request.encryptionKey().isBlank() ? null : request.encryptionKey());
        } else if (macChanged || p.getEncryptionKey() == null) {
            if (normalizedMac != null && p.getFiscalSerial() != null) {
                p.setEncryptionKey(PrinterEncryptionKeyGenerator.generateKey(p.getFiscalSerial(), normalizedMac));
            }
        }
        reconcileDistributorPaymentStatus(p, previousStatus, request.paid());
        return toResponse(repository.save(p));
    }

    @Override
    public PrinterResponse updateSoftware(Long id, Long softwareId) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        if (softwareId != null) {
            p.setSoftware(softwareRepository.findById(softwareId)
                    .orElseThrow(() -> new ResourceNotFoundException("Software not found with id: " + softwareId)));
        } else {
            p.setSoftware(null);
        }
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
    @Transactional(readOnly = true)
    public PrinterDeleteImpactResponse deleteImpact(Long id) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        List<PrinterDependencyRef> dependencies = collectDeleteDependencies(p);
        List<String> consequences = buildDeleteConsequences(dependencies);
        return new PrinterDeleteImpactResponse(
                p.getId(),
                p.getFiscalSerial(),
                dependencies,
                consequences,
                hasBlockingDependencies(dependencies));
    }

    @Override
    public void delete(Long id, boolean force) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        List<PrinterDependencyRef> dependencies = collectDeleteDependencies(p);
        boolean blocked = hasBlockingDependencies(dependencies);
        if (blocked && !force) {
            throw new PrinterDeleteBlockedException(
                    "Esta impresora tiene registros vinculados. "
                            + "Puedes forzar la eliminación si aceptas las consecuencias listadas.",
                    dependencies,
                    buildDeleteConsequences(dependencies));
        }
        if (force && blocked) {
            detachAndRemoveDependencies(p.getId());
        }
        repository.delete(p);
    }

    private static boolean hasBlockingDependencies(List<PrinterDependencyRef> dependencies) {
        return dependencies.stream().anyMatch(d -> !"client".equals(d.type()));
    }

    private void detachAndRemoveDependencies(Long printerId) {
        List<TechnicalServiceVisit> visits =
                technicalServiceVisitRepository.findByPrinter_IdOrderByCreatedAtAsc(printerId);
        for (TechnicalServiceVisit visit : visits) {
            visit.setInstalledSeal(null);
            visit.setRemovedSeal(null);
        }
        if (!visits.isEmpty()) {
            technicalServiceVisitRepository.saveAll(visits);
            technicalServiceVisitRepository.deleteAll(visits);
        }

        List<AnnualInspection> inspections =
                annualInspectionRepository.findByPrinter_IdOrderByCreatedAtAsc(printerId);
        if (!inspections.isEmpty()) {
            annualInspectionRepository.deleteAll(inspections);
        }

        List<Seal> seals = sealRepository.findByPrinter_Id(printerId);
        for (Seal seal : seals) {
            seal.setPrinter(null);
            seal.setInstallationDate(null);
            seal.setRemovalDate(null);
            seal.setStatus(SealStatus.DISPONIBLE);
        }
        if (!seals.isEmpty()) {
            sealRepository.saveAll(seals);
        }
    }

    private List<PrinterDependencyRef> collectDeleteDependencies(Printer printer) {
        Long printerId = printer.getId();
        List<PrinterDependencyRef> deps = new ArrayList<>();

        if (printer.getClient() != null && printer.getClient().getId() != null) {
            Client client = printer.getClient();
            deps.add(new PrinterDependencyRef(
                    "client",
                    client.getId(),
                    "Cliente #" + client.getId() + " (no se elimina; solo se desvincula)"));
        }

        for (Seal seal : sealRepository.findByPrinter_Id(printerId)) {
            deps.add(new PrinterDependencyRef(
                    "seal",
                    seal.getId(),
                    "Precinto " + seal.getSerial() + " (se desvincula y vuelve a disponible)"));
        }
        for (TechnicalServiceVisit visit : technicalServiceVisitRepository.findByPrinter_IdOrderByCreatedAtAsc(printerId)) {
            deps.add(new PrinterDependencyRef(
                    "technicalService",
                    visit.getId(),
                    "Servicio técnico #" + visit.getId() + " (se elimina)"));
        }
        for (AnnualInspection inspection : annualInspectionRepository.findByPrinter_IdOrderByCreatedAtAsc(printerId)) {
            String dateLabel = inspection.getInspectionDate() != null
                    ? inspection.getInspectionDate().toString()
                    : String.valueOf(inspection.getId());
            deps.add(new PrinterDependencyRef(
                    "annualInspection",
                    inspection.getId(),
                    "Inspección anual " + dateLabel + " (se elimina)"));
        }
        return deps;
    }

    private static List<String> buildDeleteConsequences(List<PrinterDependencyRef> dependencies) {
        List<String> consequences = new ArrayList<>();
        boolean hasClient = dependencies.stream().anyMatch(d -> "client".equals(d.type()));
        boolean hasSeal = dependencies.stream().anyMatch(d -> "seal".equals(d.type()));
        boolean hasService = dependencies.stream().anyMatch(d -> "technicalService".equals(d.type()));
        boolean hasInspection = dependencies.stream().anyMatch(d -> "annualInspection".equals(d.type()));

        consequences.add("La impresora desaparecerá del catálogo y de Tools/Remoto.");
        if (hasClient) {
            consequences.add("El cliente vinculado NO se eliminará; solo perderá esta impresora.");
        }
        if (hasSeal) {
            consequences.add("Los precintos instalados se desvincularán y quedarán disponibles.");
        }
        if (hasService) {
            consequences.add("Los servicios técnicos de esta impresora se borrarán de forma permanente.");
        }
        if (hasInspection) {
            consequences.add("Las inspecciones anuales de esta impresora se borrarán de forma permanente.");
        }
        if (!hasClient && !hasSeal && !hasService && !hasInspection) {
            consequences.add("No hay registros vinculados adicionales.");
        }
        return consequences;
    }

    private void assertMacAvailable(String normalizedColonMac, Long excludePrinterId) {
        if (normalizedColonMac == null) {
            return;
        }
        String compactMac = MacAddressNormalizer.requireCompactForm(normalizedColonMac);
        if (repository.existsByMacAddressCompactExcludingId(compactMac, excludePrinterId)) {
            throw new IllegalArgumentException("macAddress already exists: " + normalizedColonMac);
        }
    }

    private static String normalizeMacOrNull(String macAddress) {
        if (macAddress == null || macAddress.isBlank()) {
            return null;
        }
        return MacAddressNormalizer.toColonForm(macAddress.trim());
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
     * El estatus es la fuente de verdad del ciclo de vida; sincroniza se_pago y mantiene retrocompatibilidad
     * con payloads que aún envían paid explícito al asignar o liquidar consignación.
     */
    private void reconcileDistributorPaymentStatus(
            Printer printer,
            PrinterStatus previousStatus,
            Boolean requestedPaid) {
        PrinterStatus status = printer.getStatus();
        if (printer.getDistributor() != null) {
            if (status == PrinterStatus.ASIGNADA
                    && Boolean.FALSE.equals(requestedPaid)
                    && previousStatus != PrinterStatus.EN_CONSIGNACION) {
                status = PrinterStatus.EN_CONSIGNACION;
                printer.setStatus(status);
            } else if (status == PrinterStatus.EN_CONSIGNACION
                    && Boolean.TRUE.equals(requestedPaid)
                    && previousStatus != PrinterStatus.ASIGNADA) {
                status = PrinterStatus.ASIGNADA;
                printer.setStatus(status);
                printer.setEnajenacionAutorizada(false);
                printer.setEnajenacionAutorizadaAt(null);
                printer.setEnajenacionAutorizadaBy(null);
            } else if (status == PrinterStatus.A_CREDITO
                    && Boolean.TRUE.equals(requestedPaid)
                    && previousStatus != PrinterStatus.ENAJENADA) {
                status = PrinterStatus.ENAJENADA;
                printer.setStatus(status);
            }
        }
        printer.setPaid(status == PrinterStatus.ASIGNADA || status == PrinterStatus.ENAJENADA);
    }

    private Printer applyDisposition(Printer printer, PrinterDispositionRequest request) {
        if (printer.getStatus() != PrinterStatus.ASIGNADA
                && printer.getStatus() != PrinterStatus.EN_CONSIGNACION
                && !printer.getStatus().isDisposed()) {
            throw new IllegalArgumentException(
                    "Only assigned, consignment, or disposed printers can configure disposition ticket");
        }
        if (printer.getStatus() == PrinterStatus.ASIGNADA && !Boolean.TRUE.equals(printer.getPaid())) {
            throw new IllegalArgumentException(
                    "Solo se pueden enajenar impresoras con estatus de pago Pagada.");
        }
        if (printer.getStatus() == PrinterStatus.EN_CONSIGNACION
                && !Boolean.TRUE.equals(printer.getEnajenacionAutorizada())) {
            throw new IllegalArgumentException(
                    "Esta impresora en consignación requiere permiso aprobado del administrador antes de poder enajenarse.");
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
        if (request.installationDate() != null) {
            printer.setInstallationDate(request.installationDate());
        } else if (printer.getInstallationDate() == null
                || !printer.getStatus().isDisposed()) {
            printer.setInstallationDate(java.time.OffsetDateTime.now());
        }
        return repository.save(printer);
    }

    @Override
    public PrinterResponse authorizeEnajenacion(Long id, PrinterEnajenacionAuthorizeRequest request) {
        User admin = securityScope.currentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede dar permiso para enajenar una impresora en consignación.");
        }
        Printer printer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + id));
        if (printer.getStatus() != PrinterStatus.EN_CONSIGNACION) {
            throw new IllegalArgumentException(
                    "Solo se puede dar permiso de enajenación a impresoras en consignación.");
        }
        if (Boolean.TRUE.equals(printer.getEnajenacionAutorizada())) {
            throw new IllegalArgumentException(
                    "Esta impresora ya tiene permiso aprobado para enajenarse.");
        }

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        printer.setEnajenacionAutorizada(true);
        printer.setEnajenacionAutorizadaAt(now);
        printer.setEnajenacionAutorizadaBy(admin.getId());
        printer.setCreditInstallments(request.creditInstallments());
        if (request.finalSalePrice() != null) {
            printer.setFinalSalePrice(request.finalSalePrice());
        }
        Printer saved = repository.save(printer);

        if (modificationRequestRepository != null) {
            modificationRequestRepository
                    .findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                            ModificationTargetType.PRINTER_ENAJENACION,
                            printer.getId(),
                            ModificationRequestStatus.PENDING)
                    .ifPresent(req -> {
                        Map<String, Object> data = new java.util.LinkedHashMap<>(
                                req.getProposedData() != null ? req.getProposedData() : Map.of());
                        data.put("approvedInstallments", request.creditInstallments());
                        data.put("reviewedAt", now.toString());
                        data.put("reviewedByName", admin.getName());
                        req.setProposedData(data);
                        req.setStatus(ModificationRequestStatus.APPROVED);
                        modificationRequestRepository.save(req);
                    });
        }

        return toResponse(saved);
    }

    private com.aeg.core.firmware.Firmware resolveFirmware(Long firmwareId, String versionFirmware, Long modelId) {
        if (firmwareId != null) {
            return firmwareRepository.findById(firmwareId)
                    .orElseThrow(() -> new ResourceNotFoundException("Firmware not found with id: " + firmwareId));
        }
        if (versionFirmware != null && !versionFirmware.isBlank()) {
            String ver = versionFirmware.trim();
            if (modelId != null) {
                var byModel = firmwareRepository.findFirstByVersionAndPrinterModel_IdOrderByCreatedAtDesc(ver, modelId);
                if (byModel.isPresent()) {
                    return byModel.get();
                }
            }
            var generic = firmwareRepository.findFirstByVersionAndPrinterModelIsNullOrderByCreatedAtDesc(ver);
            if (generic.isPresent()) {
                return generic.get();
            }
            return firmwareRepository.findFirstByVersionOrderByCreatedAtDesc(ver).orElse(null);
        }
        return null;
    }

    private PrinterResponse toResponse(Printer p) {
        Long pendingRequestId = modificationRequestRepository != null && p.getId() != null
                ? modificationRequestRepository
                        .findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                                ModificationTargetType.PRINTER_ENAJENACION,
                                p.getId(),
                                ModificationRequestStatus.PENDING)
                        .map(mr -> mr.getId())
                        .orElse(null)
                : null;
        CreditTotals totals = p.getId() != null
                ? loadCreditTotalsMap(List.of(p)).get(p.getId())
                : null;
        return toResponse(p, true, pendingRequestId, totals);
    }

    private PrinterResponse toResponse(
            Printer p,
            boolean includeTicketBody,
            Long pendingEnajenacionRequestId,
            CreditTotals creditTotals) {
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
                p.getFirmwareId(),
                p.getVersionFirmware(),
                p.getMacAddress(),
                includeTicketBody ? p.getHeader() : null,
                includeTicketBody ? p.getTrailer() : null,
                p.getEncryptionKey(),
                PrinterResponse.ticketConfigured(p.getHeader()),
                p.isFirmwareUpdateUnverified(),
                Boolean.TRUE.equals(p.getEnajenacionAutorizada()),
                p.getEnajenacionAutorizadaAt(),
                p.getCreditInstallments(),
                pendingEnajenacionRequestId,
                creditTotals != null ? creditTotals.totalPaid() : BigDecimal.ZERO,
                creditTotals != null ? creditTotals.paymentCount() : 0L
        );
    }

    private Map<Long, Long> loadPendingEnajenacionRequestMap() {
        if (modificationRequestRepository == null) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : modificationRequestRepository
                .findPendingTargetIdsAndRequestIds(ModificationTargetType.PRINTER_ENAJENACION)) {
            if (row != null && row.length >= 2 && row[0] instanceof Number targetId && row[1] instanceof Number reqId) {
                map.putIfAbsent(targetId.longValue(), reqId.longValue());
            }
        }
        return map;
    }

    private Map<Long, CreditTotals> loadCreditTotalsMap(List<Printer> printers) {
        if (creditPaymentRepository == null || printers.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = printers.stream()
                .map(Printer::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, CreditTotals> map = new HashMap<>();
        for (Object[] row : creditPaymentRepository.summarizeByPrinterIds(ids)) {
            if (row != null && row.length >= 3 && row[0] instanceof Number printerId) {
                long count = row[1] instanceof Number n ? n.longValue() : 0L;
                BigDecimal total = row[2] instanceof BigDecimal b ? b : BigDecimal.ZERO;
                map.put(printerId.longValue(), new CreditTotals(count, total));
            }
        }
        return map;
    }

    private static String normalizeFiscalSerialForModel(
            String rawSerial,
            com.aeg.core.printermodel.PrinterModel model) {
        String normalized = rawSerial == null ? "" : rawSerial.trim().toUpperCase(java.util.Locale.ROOT);
        if (model != null && model.getSerialPrefix() != null && !model.getSerialPrefix().isBlank()) {
            String expectedPrefix = model.getSerialPrefix().trim().toUpperCase(java.util.Locale.ROOT);
            if (!normalized.startsWith(expectedPrefix)) {
                throw new IllegalArgumentException(
                        "fiscalSerial must start with model prefix " + expectedPrefix);
            }
        }
        return normalized;
    }

    private record CreditTotals(long paymentCount, BigDecimal totalPaid) {}
}
