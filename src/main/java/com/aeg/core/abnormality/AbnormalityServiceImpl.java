package com.aeg.core.abnormality;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.abnormality.dto.AbnormalityRequest;
import com.aeg.core.abnormality.dto.AbnormalityResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class AbnormalityServiceImpl implements AbnormalityService {

    private final AbnormalityRepository repository;
    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;

    public AbnormalityServiceImpl(
            AbnormalityRepository repository,
            PrinterRepository printerRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbnormalityResponse> findAll() {
        if (securityScope.isGlobalReader()) {
            return repository.findAllByOrderByDetectedAtDesc().stream().map(this::toResponse).toList();
        }
        List<Long> printerIds = securityScope.visiblePrinterIds();
        if (printerIds.isEmpty()) {
            return List.of();
        }
        return repository.findByPrinter_IdInOrderByDetectedAtDesc(printerIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AbnormalityResponse create(AbnormalityRequest request) {
        securityScope.assertCanWriteTechnicalService();
        Printer printer = printerRepository.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId()));
        securityScope.assertPrinterInScope(printer);

        Abnormality a = new Abnormality();
        a.setPrinter(printer);
        a.setType(request.type());
        a.setDetectedAt(request.detectedAt());
        a.setAuthorized(Boolean.TRUE.equals(request.authorized()));
        a.setUser(securityScope.currentUser());
        a.setDescription(request.description());
        a.setOrigin(AbnormalityOrigin.MANUAL);
        return toResponse(repository.save(a));
    }

    @Override
    public void record(Printer printer, AbnormalityType type, AbnormalityOrigin origin, Long originId,
            User user, String description) {
        if (originId != null && repository.existsByOriginAndOriginIdAndType(origin, originId, type)) {
            return;
        }
        Abnormality a = new Abnormality();
        a.setPrinter(printer);
        a.setType(type);
        a.setUser(user);
        a.setDescription(description);
        a.setOrigin(origin);
        a.setOriginId(originId);
        repository.save(a);
    }

    private AbnormalityResponse toResponse(Abnormality a) {
        return new AbnormalityResponse(
                a.getId(),
                a.getPrinterId(),
                a.getType(),
                a.getDetectedAt(),
                a.isAuthorized(),
                a.getUserId(),
                a.getDescription(),
                a.getOrigin(),
                a.getOriginId(),
                a.getCreatedAt());
    }
}
