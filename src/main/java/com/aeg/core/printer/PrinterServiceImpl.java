package com.aeg.core.printer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printer.dto.PrinterResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class PrinterServiceImpl implements PrinterService {

    private final PrinterRepository repository;
    private final com.aeg.core.printermodel.PrinterModelRepository modelRepository;
    private final com.aeg.core.software.SoftwareRepository softwareRepository;
    private final com.aeg.core.branch.BranchRepository branchRepository;
    private final com.aeg.core.distributor.DistributorRepository distributorRepository;
    public PrinterServiceImpl(PrinterRepository repository,
                              com.aeg.core.printermodel.PrinterModelRepository modelRepository,
                              com.aeg.core.software.SoftwareRepository softwareRepository,
                              com.aeg.core.branch.BranchRepository branchRepository,
                              com.aeg.core.distributor.DistributorRepository distributorRepository) {
        this.repository = repository;
        this.modelRepository = modelRepository;
        this.softwareRepository = softwareRepository;
        this.branchRepository = branchRepository;
        this.distributorRepository = distributorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrinterResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrinterResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Override
    public PrinterResponse create(PrinterRequest request) {
        if (repository.existsByFiscalSerialIgnoreCase(request.fiscalSerial())) {
            throw new IllegalArgumentException("fiscalSerial already exists: " + request.fiscalSerial());
        }
        Printer p = new Printer();
        if (request.modelId() != null) p.setModel(modelRepository.getReferenceById(request.modelId()));
        if (request.softwareId() != null) p.setSoftware(softwareRepository.getReferenceById(request.softwareId()));
        if (request.branchId() != null) p.setBranch(branchRepository.getReferenceById(request.branchId()));
        if (request.distributorId() != null) p.setDistributor(distributorRepository.getReferenceById(request.distributorId()));
        p.setFiscalSerial(request.fiscalSerial());
        p.setFinalSalePrice(request.finalSalePrice());
        p.setPaid(request.paid());
        p.setInstallationDate(request.installationDate());
        p.setVersionFirmware(request.versionFirmware());
        p.setMacAddress(request.macAddress());
        p.setStatus(request.status());
        p.setDeviceType(request.deviceType());
        return toResponse(repository.save(p));
    }

    @Override
    public PrinterResponse update(Long id, PrinterRequest request) {
        Printer p = findEntityById(id);
        if (!p.getFiscalSerial().equalsIgnoreCase(request.fiscalSerial()) && repository.existsByFiscalSerialIgnoreCase(request.fiscalSerial())) {
            throw new IllegalArgumentException("fiscalSerial already exists: " + request.fiscalSerial());
        }
        if (request.modelId() != null) p.setModel(modelRepository.getReferenceById(request.modelId()));
        if (request.softwareId() != null) p.setSoftware(softwareRepository.getReferenceById(request.softwareId()));
        if (request.branchId() != null) p.setBranch(branchRepository.getReferenceById(request.branchId()));
        if (request.distributorId() != null) p.setDistributor(distributorRepository.getReferenceById(request.distributorId()));
        p.setFiscalSerial(request.fiscalSerial());
        p.setFinalSalePrice(request.finalSalePrice());
        p.setPaid(request.paid());
        p.setInstallationDate(request.installationDate());
        p.setVersionFirmware(request.versionFirmware());
        p.setMacAddress(request.macAddress());
        p.setStatus(request.status());
        p.setDeviceType(request.deviceType());
        return toResponse(repository.save(p));
    }

    @Override
    public void delete(Long id) {
        Printer p = findEntityById(id);
        repository.delete(p);
    }

    private Printer findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + id));
    }

    private PrinterResponse toResponse(Printer p) {
        return new PrinterResponse(
                p.getId(),
                p.getModelId(),
                p.getSoftwareId(),
                p.getBranchId(),
                p.getFiscalSerial(),
                p.getFinalSalePrice(),
                p.getCreatedAt(),
                p.getStatus(),
                p.getDistributorId(),
                p.getPaid(),
                p.getInstallationDate(),
                p.getVersionFirmware(),
                p.getMacAddress(),
                p.getDeviceType()
        );
    }
}
