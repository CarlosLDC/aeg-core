package com.aeg.core.decommission;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.abnormality.AbnormalityOrigin;
import com.aeg.core.abnormality.AbnormalityService;
import com.aeg.core.abnormality.AbnormalityType;
import com.aeg.core.decommission.dto.DecommissionRequest;
import com.aeg.core.decommission.dto.DecommissionResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class DecommissionServiceImpl implements DecommissionService {

    private final PrinterDecommissionRepository repository;
    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;
    private final AbnormalityService abnormalityService;

    public DecommissionServiceImpl(
            PrinterDecommissionRepository repository,
            PrinterRepository printerRepository,
            SecurityScopeService securityScope,
            AbnormalityService abnormalityService) {
        this.repository = repository;
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
        this.abnormalityService = abnormalityService;
    }

    @Override
    public DecommissionResponse decommission(Long printerId, DecommissionRequest request) {
        securityScope.assertCanWriteOperationalData();
        Printer printer = findPrinter(printerId);
        securityScope.assertPrinterInScope(printer);
        if (printer.getStatus() == PrinterStatus.DESINCORPORADA) {
            throw new IllegalArgumentException("Printer is already decommissioned");
        }
        User actor = securityScope.currentUser();

        PrinterDecommission d = new PrinterDecommission();
        d.setPrinter(printer);
        d.setReason(request.reason());
        d.setDate(request.date());
        d.setOwnPrinter(Boolean.TRUE.equals(request.ownPrinter()));
        d.setUser(actor);
        d.setNotes(request.notes());
        PrinterDecommission saved = repository.save(d);

        printer.setStatus(PrinterStatus.DESINCORPORADA);
        printerRepository.save(printer);

        if (request.reason() == DecommissionReason.PERDIDA) {
            abnormalityService.record(printer, AbnormalityType.PERDIDA, AbnormalityOrigin.MANUAL,
                    saved.getId(), actor, request.notes());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecommissionResponse> findByPrinter(Long printerId) {
        Printer printer = findPrinter(printerId);
        securityScope.assertPrinterInScope(printer);
        return repository.findByPrinter_IdOrderByDateDescIdDesc(printerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Printer findPrinter(Long printerId) {
        return printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));
    }

    private DecommissionResponse toResponse(PrinterDecommission d) {
        return new DecommissionResponse(
                d.getId(),
                d.getPrinterId(),
                d.getDate(),
                d.getReason(),
                d.isOwnPrinter(),
                d.getUserId(),
                d.getNotes(),
                d.getCreatedAt());
    }
}
