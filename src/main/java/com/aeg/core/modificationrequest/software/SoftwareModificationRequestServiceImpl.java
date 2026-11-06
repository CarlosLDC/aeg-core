package com.aeg.core.modificationrequest.software;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequest;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationCreateRequest;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.software.dto.SoftwareModificationRequestListItemResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.software.Software;
import com.aeg.core.software.SoftwareRepository;
import com.aeg.core.software.SoftwareService;
import com.aeg.core.software.dto.SoftwareDistributorResponse;
import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;

@Service
@Transactional
public class SoftwareModificationRequestServiceImpl implements SoftwareModificationRequestService {

    private final ModificationRequestRepository repository;
    private final SoftwareRepository softwareRepository;
    private final SoftwareService softwareService;
    private final PrinterRepository printerRepository;
    private final CompanyRepository companyRepository;
    private final SecurityScopeService securityScope;

    public SoftwareModificationRequestServiceImpl(
            ModificationRequestRepository repository,
            SoftwareRepository softwareRepository,
            SoftwareService softwareService,
            PrinterRepository printerRepository,
            CompanyRepository companyRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.softwareRepository = softwareRepository;
        this.softwareService = softwareService;
        this.printerRepository = printerRepository;
        this.companyRepository = companyRepository;
        this.securityScope = securityScope;
    }

    @Override
    public SoftwareModificationRequestDetailResponse createRequest(SoftwareModificationCreateRequest request) {
        Printer printer = printerRepository.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId()));
        securityScope.assertPrinterInScope(printer);

        String trimmedName = request.name().trim();
        String trimmedVersion = request.version().trim();

        if (softwareRepository.existsByNameIgnoreCaseAndVersionIgnoreCase(trimmedName, trimmedVersion)) {
            throw new IllegalArgumentException("El software ya existe en el sistema. Puedes seleccionarlo directamente.");
        }

        Optional<ModificationRequest> existing = repository.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.SOFTWARE,
                printer.getId(),
                ModificationRequestStatus.PENDING);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Ya existe una solicitud pendiente de software para esta impresora.");
        }

        List<Long> distributorCompanyIds = new ArrayList<>(
                request.distributorCompanyIds() == null ? List.of() : request.distributorCompanyIds());
        if (printer.getDistributor() != null
                && printer.getDistributor().getBranch() != null
                && printer.getDistributor().getBranch().getCompany() != null) {
            Long printerCompanyId = printer.getDistributor().getBranch().getCompany().getId();
            if (printerCompanyId != null && !distributorCompanyIds.contains(printerCompanyId)) {
                distributorCompanyIds.add(printerCompanyId);
            }
        }

        Map<String, Object> proposedData = new LinkedHashMap<>();
        proposedData.put("name", trimmedName);
        proposedData.put("version", trimmedVersion);
        proposedData.put("external", Boolean.TRUE.equals(request.external()));
        proposedData.put("developerName", request.developerName());
        proposedData.put("developerRif", request.developerRif());
        proposedData.put("programmingLanguages", request.programmingLanguages() == null ? List.of() : request.programmingLanguages());
        proposedData.put("operatingSystems", request.operatingSystems() == null ? List.of() : request.operatingSystems());
        proposedData.put("distributorCompanyIds", distributorCompanyIds);
        proposedData.put("printerId", printer.getId());

        ModificationRequest mr = new ModificationRequest();
        mr.setTargetType(ModificationTargetType.SOFTWARE);
        mr.setTargetId(printer.getId());
        mr.setActionType(ModificationActionType.CREATE);
        mr.setProposedData(proposedData);
        mr.setRequestedBy(securityScope.currentUser());
        mr.setStatus(ModificationRequestStatus.PENDING);

        ModificationRequest saved = repository.save(mr);
        return toDetailResponse(saved, printer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareModificationRequestListItemResponse> findByStatus(ModificationRequestStatus status) {
        ModificationRequestStatus target = status == null ? ModificationRequestStatus.PENDING : status;
        List<ModificationRequest> requests = repository.findByTargetTypeAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.SOFTWARE,
                target);

        return requests.stream().map(req -> {
            Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
            String fiscalSerial = printer != null ? printer.getFiscalSerial() : "—";
            Map<String, Object> data = req.getProposedData();
            String softwareName = data != null && data.get("name") != null ? String.valueOf(data.get("name")) : "—";
            String softwareVersion = data != null && data.get("version") != null ? String.valueOf(data.get("version")) : "—";
            String requesterName = req.getRequestedBy() != null ? req.getRequestedBy().getName() : "—";

            return new SoftwareModificationRequestListItemResponse(
                    req.getId(),
                    req.getTargetId(),
                    fiscalSerial,
                    softwareName,
                    softwareVersion,
                    req.getActionType(),
                    req.getStatus(),
                    req.getRequestedBy() != null ? req.getRequestedBy().getId() : null,
                    requesterName,
                    req.getCreatedAt());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SoftwareModificationRequestDetailResponse findById(Long id) {
        ModificationRequest req = findRequest(id);
        Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
        return toDetailResponse(req, printer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SoftwareModificationRequestDetailResponse> findPendingByPrinterId(Long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        securityScope.assertPrinterInScope(printer);

        return repository.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.SOFTWARE,
                printerId,
                ModificationRequestStatus.PENDING)
                .map(req -> toDetailResponse(req, printer));
    }

    @Override
    public SoftwareModificationRequestDetailResponse approve(Long id) {
        ModificationRequest req = findRequest(id);
        assertPending(req);

        Map<String, Object> data = req.getProposedData();
        String name = String.valueOf(data.get("name")).trim();
        String version = String.valueOf(data.get("version")).trim();
        Boolean external = Boolean.valueOf(String.valueOf(data.get("external")));
        String developerName = data.get("developerName") != null ? String.valueOf(data.get("developerName")) : null;
        String developerRif = data.get("developerRif") != null ? String.valueOf(data.get("developerRif")) : null;

        @SuppressWarnings("unchecked")
        List<String> programmingLanguages = data.get("programmingLanguages") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        @SuppressWarnings("unchecked")
        List<String> operatingSystems = data.get("operatingSystems") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        List<Long> distributorCompanyIds = new ArrayList<>();
        if (data.get("distributorCompanyIds") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number n) {
                    distributorCompanyIds.add(n.longValue());
                } else if (item != null) {
                    try {
                        distributorCompanyIds.add(Long.parseLong(String.valueOf(item)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Create or find existing software
        Software software = softwareRepository.findByNameIgnoreCaseAndVersionIgnoreCase(name, version)
                .orElseGet(() -> {
                    SoftwareRequest swReq = new SoftwareRequest(
                            name,
                            version,
                            programmingLanguages,
                            operatingSystems,
                            developerName,
                            developerRif,
                            external,
                            distributorCompanyIds);
                    SoftwareResponse created = softwareService.create(swReq);
                    return softwareRepository.findById(created.id())
                            .orElseThrow(() -> new ResourceNotFoundException("Created software not found with id: " + created.id()));
                });

        // Automatically assign to target printer
        Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
        if (printer != null) {
            printer.setSoftware(software);
            printerRepository.save(printer);
        }

        req.setStatus(ModificationRequestStatus.APPROVED);
        ModificationRequest saved = repository.save(req);
        return toDetailResponse(saved, printer);
    }

    @Override
    public SoftwareModificationRequestDetailResponse reject(Long id) {
        ModificationRequest req = findRequest(id);
        assertPending(req);
        req.setStatus(ModificationRequestStatus.REJECTED);
        ModificationRequest saved = repository.save(req);
        Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
        return toDetailResponse(saved, printer);
    }

    @Override
    public SoftwareModificationRequestDetailResponse cancel(Long id) {
        ModificationRequest req = findRequest(id);
        assertPending(req);
        assertRequesterOrAdmin(req);
        req.setStatus(ModificationRequestStatus.REJECTED);
        ModificationRequest saved = repository.save(req);
        Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
        return toDetailResponse(saved, printer);
    }

    private ModificationRequest findRequest(Long id) {
        ModificationRequest req = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modification request not found with id: " + id));
        if (req.getTargetType() != ModificationTargetType.SOFTWARE) {
            throw new ResourceNotFoundException("Modification request not found with id: " + id);
        }
        return req;
    }

    private void assertPending(ModificationRequest req) {
        if (req.getStatus() != ModificationRequestStatus.PENDING) {
            throw new IllegalStateException("Modification request is not pending: " + req.getStatus());
        }
    }

    private void assertRequesterOrAdmin(ModificationRequest req) {
        User user = securityScope.currentUser();
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (req.getRequestedBy() == null || !req.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not allowed to cancel this modification request");
        }
    }

    private SoftwareModificationRequestDetailResponse toDetailResponse(ModificationRequest req, Printer printer) {
        Map<String, Object> data = req.getProposedData() != null ? req.getProposedData() : Map.of();
        String name = data.get("name") != null ? String.valueOf(data.get("name")) : "";
        String version = data.get("version") != null ? String.valueOf(data.get("version")) : "";
        boolean external = Boolean.parseBoolean(String.valueOf(data.get("external")));
        String developerName = data.get("developerName") != null ? String.valueOf(data.get("developerName")) : null;
        String developerRif = data.get("developerRif") != null ? String.valueOf(data.get("developerRif")) : null;

        @SuppressWarnings("unchecked")
        List<String> programmingLanguages = data.get("programmingLanguages") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        @SuppressWarnings("unchecked")
        List<String> operatingSystems = data.get("operatingSystems") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        List<Long> companyIds = new ArrayList<>();
        if (data.get("distributorCompanyIds") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number n) {
                    companyIds.add(n.longValue());
                } else if (item != null) {
                    try {
                        companyIds.add(Long.parseLong(String.valueOf(item)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        List<SoftwareDistributorResponse> distributors = companyIds.isEmpty()
                ? List.of()
                : companyRepository.findAllById(companyIds).stream()
                        .map(c -> new SoftwareDistributorResponse(c.getId(), c.getRif(), c.getBusinessName()))
                        .toList();

        String printerSerial = printer != null ? printer.getFiscalSerial() : "—";
        String modelCode = printer != null && printer.getModel() != null ? printer.getModel().getModelCode() : "—";
        String distributorName = printer != null
                && printer.getDistributor() != null
                && printer.getDistributor().getBranch() != null
                && printer.getDistributor().getBranch().getCompany() != null
                ? printer.getDistributor().getBranch().getCompany().getBusinessName()
                : "—";

        String requesterName = req.getRequestedBy() != null ? req.getRequestedBy().getName() : "—";

        return new SoftwareModificationRequestDetailResponse(
                req.getId(),
                req.getTargetId(),
                printerSerial,
                modelCode,
                distributorName,
                req.getActionType(),
                req.getStatus(),
                name,
                version,
                programmingLanguages,
                operatingSystems,
                developerName,
                developerRif,
                external,
                distributors,
                req.getRequestedBy() != null ? req.getRequestedBy().getId() : null,
                requesterName,
                req.getCreatedAt());
    }
}
