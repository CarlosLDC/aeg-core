package com.aeg.core.printer;

import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.dto.PrinterRequest;
import com.aeg.core.printer.dto.PrinterResponse;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class PrinterServiceImpl implements PrinterService {

    private final PrinterRepository repository;
    private final com.aeg.core.printermodel.PrinterModelRepository modelRepository;
    private final com.aeg.core.software.SoftwareRepository softwareRepository;
    private final com.aeg.core.distributor.DistributorRepository distributorRepository;
    private final com.aeg.core.client.ClientRepository clientRepository;
    private final SecurityScopeService securityScope;

    public PrinterServiceImpl(PrinterRepository repository,
                              com.aeg.core.printermodel.PrinterModelRepository modelRepository,
                              com.aeg.core.software.SoftwareRepository softwareRepository,
                              com.aeg.core.distributor.DistributorRepository distributorRepository,
                              com.aeg.core.client.ClientRepository clientRepository,
                              SecurityScopeService securityScope) {
        this.repository = repository;
        this.modelRepository = modelRepository;
        this.softwareRepository = softwareRepository;
        this.distributorRepository = distributorRepository;
        this.clientRepository = clientRepository;
        this.securityScope = securityScope;
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
        return toResponse(repository.save(p));
    }

    @Override
    public PrinterResponse update(Long id, PrinterRequest request) {
        Printer p = findEntityById(id);
        securityScope.assertPrinterInScope(p);
        if (currentUser().getRole() == Role.DISTRIBUTOR) {
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
        return toResponse(repository.save(p));
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
        if (user.getRole() != Role.DISTRIBUTOR) {
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
        } else if (currentUser().getRole() != Role.DISTRIBUTOR) {
            printer.setDistributor(null);
        }
    }

    /**
     * Distribuidor: única mutación permitida — enajenar impresora asignada a un cliente propio.
     */
    private Printer applyDistributorDisposition(Printer printer, PrinterRequest request) {
        if (printer.getStatus() != PrinterStatus.ASIGNADA) {
            throw new IllegalArgumentException("Only assigned printers can be disposed to a client");
        }
        if (!Boolean.TRUE.equals(printer.getPaid())) {
            throw new IllegalArgumentException(
                    "Solo se pueden enajenar impresoras con estatus de pago Pagada.");
        }
        if (request.status() != PrinterStatus.ENAJENADA) {
            throw new AccessDeniedException("Distributors can only dispose assigned printers to a client");
        }
        if (request.clientId() == null) {
            throw new IllegalArgumentException("clientId is required to dispose a printer");
        }
        assertDispositionFieldsUnchanged(printer, request);

        var client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
        securityScope.assertClientInScope(client);
        printer.setClient(client);
        printer.setStatus(PrinterStatus.ENAJENADA);
        printer.setInstallationDate(
                request.installationDate() != null
                        ? request.installationDate()
                        : java.time.OffsetDateTime.now());
        return repository.save(printer);
    }

    private void assertDispositionFieldsUnchanged(Printer printer, PrinterRequest request) {
        Long ownDistributorId = resolveDistributorIdForWrite(request.distributorId());
        if (!Objects.equals(printer.getDistributorId(), ownDistributorId)) {
            throw new AccessDeniedException("Cannot reassign printer to another distributor");
        }
        if (!Objects.equals(printer.getModelId(), request.modelId())
                || !Objects.equals(printer.getSoftwareId(), request.softwareId())
                || !printer.getFiscalSerial().equalsIgnoreCase(request.fiscalSerial())
                || !sameDecimal(printer.getFinalSalePrice(), request.finalSalePrice())
                || !Objects.equals(printer.getPaid(), request.paid())
                || !Objects.equals(printer.getVersionFirmware(), request.versionFirmware())
                || !Objects.equals(normalizeMac(printer.getMacAddress()), normalizeMac(request.macAddress()))
                || printer.getDeviceType() != request.deviceType()) {
            throw new AccessDeniedException(
                    "Distributors can only change printer status to disposed and assign a client");
        }
    }

    private static boolean sameDecimal(java.math.BigDecimal left, java.math.BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private static String normalizeMac(String mac) {
        if (mac == null || mac.isBlank()) {
            return null;
        }
        return mac.trim().toUpperCase();
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
