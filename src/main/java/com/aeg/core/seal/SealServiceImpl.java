package com.aeg.core.seal;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.Printer;
import com.aeg.core.seal.dto.SealRequest;
import com.aeg.core.seal.dto.SealResponse;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class SealServiceImpl implements SealService {

    private final SealRepository repository;
    private final com.aeg.core.printer.PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;

    public SealServiceImpl(
            SealRepository repository,
            com.aeg.core.printer.PrinterRepository printerRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SealResponse> findAll() {
        return securityScope.findVisibleSeals().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SealResponse findById(Long id) {
        Seal seal = findEntityById(id);
        securityScope.assertSealInScope(seal);
        return toResponse(seal);
    }

    @Override
    public SealResponse create(SealRequest request) {
        if (repository.existsBySerialIgnoreCase(request.serial())) {
            throw new IllegalArgumentException("serial already exists: " + request.serial());
        }
        Seal s = new Seal();
        if (request.printerId() != null) {
            Printer printer = printerRepository.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId()));
            securityScope.assertPrinterInScope(printer);
            s.setPrinter(printer);
        }
        s.setSerial(request.serial());
        s.setInstallationDate(request.installationDate());
        s.setRemovalDate(request.removalDate());
        s.setColor(request.color());
        s.setStatus(request.status());
        s.setCreationBatchId(request.creationBatchId());
        return toResponse(repository.save(s));
    }

    @Override
    public SealResponse update(Long id, SealRequest request) {
        Seal s = findEntityById(id);
        securityScope.assertSealInScope(s);
        if (!s.getSerial().equalsIgnoreCase(request.serial()) && repository.existsBySerialIgnoreCase(request.serial())) {
            throw new IllegalArgumentException("serial already exists: " + request.serial());
        }
        if (request.printerId() != null) {
            Printer printer = printerRepository.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId()));
            securityScope.assertPrinterInScope(printer);
            s.setPrinter(printer);
        }
        s.setSerial(request.serial());
        s.setInstallationDate(request.installationDate());
        s.setRemovalDate(request.removalDate());
        s.setColor(request.color());
        s.setStatus(request.status());
        return toResponse(repository.save(s));
    }

    @Override
    public void delete(Long id) {
        Seal s = findEntityById(id);
        securityScope.assertSealInScope(s);
        repository.delete(s);
    }

    private Seal findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seal not found with id: " + id));
    }

    private SealResponse toResponse(Seal s) {
        return new SealResponse(
                s.getId(),
                s.getPrinterId(),
                s.getSerial(),
                s.getCreatedAt(),
                s.getCreationBatchId(),
                s.getInstallationDate(),
                s.getRemovalDate(),
                s.getColor(),
                s.getStatus());
    }
}
