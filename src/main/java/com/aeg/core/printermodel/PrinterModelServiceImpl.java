package com.aeg.core.printermodel;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printermodel.dto.PrinterModelRequest;
import com.aeg.core.printermodel.dto.PrinterModelResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class PrinterModelServiceImpl implements PrinterModelService {

    private final PrinterModelRepository repository;

    public PrinterModelServiceImpl(PrinterModelRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrinterModelResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterModelResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Override
    public PrinterModelResponse create(PrinterModelRequest request) {
        PrinterModel m = new PrinterModel();
        m.setBrand(request.brand());
        m.setModelCode(request.modelCode());
        m.setProvidencia(request.providencia());
        m.setApprovalDate(request.approvalDate());
        m.setPrice(request.price());
        return toResponse(repository.save(m));
    }

    @Override
    public PrinterModelResponse update(Long id, PrinterModelRequest request) {
        PrinterModel m = findEntityById(id);
        m.setBrand(request.brand());
        m.setModelCode(request.modelCode());
        m.setProvidencia(request.providencia());
        m.setApprovalDate(request.approvalDate());
        m.setPrice(request.price());
        return toResponse(repository.save(m));
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
        return new PrinterModelResponse(m.getId(), m.getBrand(), m.getModelCode(), m.getProvidencia(), m.getApprovalDate(), m.getCreatedAt(), m.getPrice());
    }
}
