package com.aeg.core.printermodel;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printermodel.dto.PrinterModelRequest;
import com.aeg.core.printermodel.dto.PrinterModelResponse;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class PrinterModelServiceImpl implements PrinterModelService {

    private final PrinterModelRepository repository;
    private final PrinterRepository printerRepository;
    private final SecurityScopeService securityScope;

    public PrinterModelServiceImpl(
            PrinterModelRepository repository,
            PrinterRepository printerRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.printerRepository = printerRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrinterModelResponse> findAll() {
        User user = securityScope.currentUser();
        if (Role.isDistributorScoped(user.getRole()) && user.getDistributorId() != null) {
            Set<Long> modelIds = printerRepository.findByDistributor_Id(user.getDistributorId()).stream()
                    .map(p -> p.getModelId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (modelIds.isEmpty()) {
                return List.of();
            }
            return repository.findAllById(modelIds).stream().map(this::toResponse).toList();
        }
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterModelResponse findById(Long id) {
        PrinterModel m = findEntityById(id);
        assertDistributorCanReadModel(m.getId());
        return toResponse(m);
    }

    private void assertDistributorCanReadModel(Long modelId) {
        User user = securityScope.currentUser();
        if (!Role.isDistributorScoped(user.getRole()) || user.getDistributorId() == null) {
            return;
        }
        boolean allowed = printerRepository.findByDistributor_Id(user.getDistributorId()).stream()
                .anyMatch(p -> modelId.equals(p.getModelId()));
        if (!allowed) {
            throw new AccessDeniedException("Not allowed to access this printer model");
        }
    }

    @Override
    public PrinterModelResponse create(PrinterModelRequest request) {
        String prefix = normalizeSerialPrefix(request.serialPrefix());
        if (repository.existsBySerialPrefixIgnoreCase(prefix)) {
            throw new IllegalArgumentException("serialPrefix already exists: " + prefix);
        }
        PrinterModel m = new PrinterModel();
        m.setModelCode(request.modelCode().trim());
        m.setSerialPrefix(prefix);
        m.setProvidencia(request.providencia());
        m.setApprovalDate(request.approvalDate());
        m.setPrice(request.price());
        return toResponse(repository.save(m));
    }

    @Override
    public PrinterModelResponse update(Long id, PrinterModelRequest request) {
        PrinterModel m = findEntityById(id);
        String prefix = normalizeSerialPrefix(request.serialPrefix());
        if (repository.existsBySerialPrefixIgnoreCaseAndIdNot(prefix, id)) {
            throw new IllegalArgumentException("serialPrefix already exists: " + prefix);
        }
        m.setModelCode(request.modelCode().trim());
        m.setSerialPrefix(prefix);
        m.setProvidencia(request.providencia());
        m.setApprovalDate(request.approvalDate());
        m.setPrice(request.price());
        return toResponse(repository.save(m));
    }

    private static String normalizeSerialPrefix(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public void delete(Long id) {
        PrinterModel m = findEntityById(id);
        repository.delete(m);
    }

    private PrinterModel findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PrinterModel not found with id: " + id));
    }

    private PrinterModelResponse toResponse(PrinterModel m) {
        return new PrinterModelResponse(
                m.getId(),
                m.getModelCode(),
                m.getSerialPrefix(),
                m.getProvidencia(),
                m.getApprovalDate(),
                m.getCreatedAt(),
                m.getPrice());
    }
}
