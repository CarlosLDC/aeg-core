package com.aeg.core.modificationrequest.printer;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.modificationrequest.ModificationActionType;
import com.aeg.core.modificationrequest.ModificationRequest;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.modificationrequest.ModificationRequestStatus;
import com.aeg.core.modificationrequest.ModificationTargetType;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionApproveRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionCreateRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRejectRequest;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRequestDetailResponse;
import com.aeg.core.modificationrequest.printer.dto.PrinterEnajenacionRequestListItemResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class PrinterEnajenacionRequestServiceImpl implements PrinterEnajenacionRequestService {

    private final ModificationRequestRepository repository;
    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;

    public PrinterEnajenacionRequestServiceImpl(
            ModificationRequestRepository repository,
            PrinterRepository printerRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
    }

    @Override
    public PrinterEnajenacionRequestDetailResponse createRequest(PrinterEnajenacionCreateRequest request) {
        User currentUser = securityScope.currentUser();
        if (currentUser.getRole() != com.aeg.core.security.Role.ADMIN) {
            throw new AccessDeniedException(
                    "Los distribuidores no pueden solicitar permiso para enajenar impresoras en consignación.");
        }
        Printer printer = printerRepository.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId()));
        securityScope.assertPrinterInScope(printer);

        if (printer.getStatus() != PrinterStatus.EN_CONSIGNACION) {
            throw new IllegalArgumentException(
                    "Solo se puede solicitar permiso de enajenación para impresoras en consignación.");
        }
        if (Boolean.TRUE.equals(printer.getEnajenacionAutorizada())) {
            throw new IllegalArgumentException(
                    "Esta impresora ya tiene permiso aprobado para enajenarse.");
        }

        Optional<ModificationRequest> existing = repository
                .findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                        ModificationTargetType.PRINTER_ENAJENACION,
                        printer.getId(),
                        ModificationRequestStatus.PENDING);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una solicitud de permiso de enajenación pendiente para esta impresora.");
        }

        Map<String, Object> proposedData = new LinkedHashMap<>();
        proposedData.put("fiscalSerial", printer.getFiscalSerial());
        if (printer.getDistributorId() != null) {
            proposedData.put("distributorId", printer.getDistributorId());
        }
        if (request.suggestedInstallments() != null) {
            proposedData.put("suggestedInstallments", request.suggestedInstallments());
        }
        if (request.notes() != null && !request.notes().isBlank()) {
            proposedData.put("notes", request.notes().trim());
        }

        ModificationRequest mr = new ModificationRequest();
        mr.setTargetType(ModificationTargetType.PRINTER_ENAJENACION);
        mr.setTargetId(printer.getId());
        mr.setActionType(ModificationActionType.UPDATE);
        mr.setProposedData(proposedData);
        mr.setRequestedBy(securityScope.currentUser());
        mr.setStatus(ModificationRequestStatus.PENDING);

        ModificationRequest saved = repository.save(mr);
        return toDetailResponse(saved, printer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrinterEnajenacionRequestListItemResponse> findByStatus(ModificationRequestStatus status) {
        ModificationRequestStatus target = status == null ? ModificationRequestStatus.PENDING : status;
        List<ModificationRequest> requests = repository.findByTargetTypeAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.PRINTER_ENAJENACION,
                target);

        return requests.stream().map(req -> {
            Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
            Map<String, Object> data = req.getProposedData() != null ? req.getProposedData() : Map.of();
            String fiscalSerial = printer != null
                    ? printer.getFiscalSerial()
                    : stringOrFallback(data.get("fiscalSerial"), "—");
            String distributorName = resolveDistributorName(printer);
            PrinterStatus printerStatus = printer != null ? printer.getStatus() : null;
            Integer suggestedInstallments = toInteger(data.get("suggestedInstallments"));
            String notes = stringOrNull(data.get("notes"));
            String requesterName = req.getRequestedBy() != null ? req.getRequestedBy().getName() : "—";

            return new PrinterEnajenacionRequestListItemResponse(
                    req.getId(),
                    req.getTargetId(),
                    fiscalSerial,
                    distributorName,
                    printerStatus,
                    suggestedInstallments,
                    notes,
                    req.getActionType(),
                    req.getStatus(),
                    req.getRequestedBy() != null ? req.getRequestedBy().getId() : null,
                    requesterName,
                    req.getCreatedAt());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterEnajenacionRequestDetailResponse findById(Long id) {
        ModificationRequest req = findRequest(id);
        Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
        User user = securityScope.currentUser();
        if (user.getRole() != Role.ADMIN) {
            if (printer != null) {
                securityScope.assertPrinterInScope(printer);
            } else if (req.getRequestedBy() == null || !req.getRequestedBy().getId().equals(user.getId())) {
                throw new AccessDeniedException("No tienes permiso para ver esta solicitud.");
            }
        }
        return toDetailResponse(req, printer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrinterEnajenacionRequestDetailResponse> findPendingByPrinterId(Long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
        securityScope.assertPrinterInScope(printer);

        return repository.findFirstByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                ModificationTargetType.PRINTER_ENAJENACION,
                printerId,
                ModificationRequestStatus.PENDING)
                .map(req -> toDetailResponse(req, printer));
    }

    @Override
    public PrinterEnajenacionRequestDetailResponse approve(Long id, PrinterEnajenacionApproveRequest request) {
        User admin = securityScope.currentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede aprobar solicitudes de enajenación.");
        }

        ModificationRequest req = findRequest(id);
        assertPending(req);

        Printer printer = printerRepository.findById(req.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + req.getTargetId()));
        if (printer.getStatus() != PrinterStatus.EN_CONSIGNACION) {
            throw new IllegalStateException(
                    "Solo se puede autorizar la enajenación si la impresora sigue en consignación (estatus actual: "
                            + printer.getStatus().getValue() + ").");
        }

        OffsetDateTime now = OffsetDateTime.now();
        printer.setEnajenacionAutorizada(true);
        printer.setEnajenacionAutorizadaAt(now);
        printer.setEnajenacionAutorizadaBy(admin.getId());
        printer.setCreditInstallments(request.creditInstallments());
        if (request.finalSalePrice() != null) {
            printer.setFinalSalePrice(request.finalSalePrice());
        }
        printerRepository.save(printer);

        Map<String, Object> data = new LinkedHashMap<>(
                req.getProposedData() != null ? req.getProposedData() : Map.of());
        data.put("approvedInstallments", request.creditInstallments());
        if (request.reviewNote() != null && !request.reviewNote().isBlank()) {
            data.put("reviewNote", request.reviewNote().trim());
        }
        data.put("reviewedAt", now.toString());
        data.put("reviewedByName", admin.getName());

        req.setProposedData(data);
        req.setStatus(ModificationRequestStatus.APPROVED);
        ModificationRequest saved = repository.save(req);
        return toDetailResponse(saved, printer);
    }

    @Override
    public PrinterEnajenacionRequestDetailResponse reject(Long id, PrinterEnajenacionRejectRequest request) {
        User admin = securityScope.currentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede rechazar solicitudes de enajenación.");
        }

        ModificationRequest req = findRequest(id);
        assertPending(req);

        Map<String, Object> data = new LinkedHashMap<>(
                req.getProposedData() != null ? req.getProposedData() : Map.of());
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            data.put("reviewNote", request.reason().trim());
        }
        data.put("reviewedAt", OffsetDateTime.now().toString());
        data.put("reviewedByName", admin.getName());

        req.setProposedData(data);
        req.setStatus(ModificationRequestStatus.REJECTED);
        ModificationRequest saved = repository.save(req);
        Printer printer = printerRepository.findById(req.getTargetId()).orElse(null);
        return toDetailResponse(saved, printer);
    }

    @Override
    public PrinterEnajenacionRequestDetailResponse cancel(Long id) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Printer enajenacion request not found with id: " + id));
        if (req.getTargetType() != ModificationTargetType.PRINTER_ENAJENACION) {
            throw new ResourceNotFoundException("Printer enajenacion request not found with id: " + id);
        }
        return req;
    }

    private void assertPending(ModificationRequest req) {
        if (req.getStatus() != ModificationRequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud no está pendiente: " + req.getStatus());
        }
    }

    private void assertRequesterOrAdmin(ModificationRequest req) {
        User user = securityScope.currentUser();
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (req.getRequestedBy() == null || !req.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("No tienes permiso para cancelar esta solicitud.");
        }
    }

    private PrinterEnajenacionRequestDetailResponse toDetailResponse(ModificationRequest req, Printer printer) {
        Map<String, Object> data = req.getProposedData() != null ? req.getProposedData() : Map.of();
        String fiscalSerial = printer != null
                ? printer.getFiscalSerial()
                : stringOrFallback(data.get("fiscalSerial"), "—");
        String modelCode = printer != null && printer.getModel() != null
                ? printer.getModel().getModelCode()
                : "—";
        String distributorName = resolveDistributorName(printer);
        String requesterName = req.getRequestedBy() != null ? req.getRequestedBy().getName() : "—";

        return new PrinterEnajenacionRequestDetailResponse(
                req.getId(),
                req.getTargetId(),
                fiscalSerial,
                modelCode,
                printer != null ? printer.getDistributorId() : null,
                distributorName,
                printer != null ? printer.getStatus() : null,
                printer != null ? printer.getFinalSalePrice() : null,
                printer != null ? printer.getCreditInstallments() : null,
                printer != null ? printer.getEnajenacionAutorizada() : null,
                toInteger(data.get("suggestedInstallments")),
                stringOrNull(data.get("notes")),
                toInteger(data.get("approvedInstallments")),
                stringOrNull(data.get("reviewNote")),
                req.getActionType(),
                req.getStatus(),
                req.getRequestedBy() != null ? req.getRequestedBy().getId() : null,
                requesterName,
                req.getCreatedAt());
    }

    private static String resolveDistributorName(Printer printer) {
        if (printer == null || printer.getDistributor() == null) {
            return "—";
        }
        if (printer.getDistributor().getBranch() != null
                && printer.getDistributor().getBranch().getCompany() != null) {
            return printer.getDistributor().getBranch().getCompany().getBusinessName();
        }
        return "Distribuidor #" + printer.getDistributorId();
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String stringOrNull(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static String stringOrFallback(Object value, String fallback) {
        String s = stringOrNull(value);
        return s != null ? s : fallback;
    }
}
