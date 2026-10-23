package com.aeg.core.fiscalizacion.mqtt;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterEncryptionKeyGenerator;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;

@Service
public class FiscalizacionCompletionService {

    private final PrinterRepository printerRepository;
    private final SealRepository sealRepository;
    private final DistributorRepository distributorRepository;
    private final FiscalizacionSessionRegistry sessionRegistry;

    public FiscalizacionCompletionService(
            PrinterRepository printerRepository,
            SealRepository sealRepository) {
        this(printerRepository, sealRepository, null, null);
    }

    @Autowired
    public FiscalizacionCompletionService(
            PrinterRepository printerRepository,
            SealRepository sealRepository,
            DistributorRepository distributorRepository,
            FiscalizacionSessionRegistry sessionRegistry) {
        this.printerRepository = printerRepository;
        this.sealRepository = sealRepository;
        this.distributorRepository = distributorRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional
    public Printer complete(FiscalizacionContext context) {
        if (printerRepository.existsByFiscalSerialIgnoreCase(context.ptrReg())) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_REGISTRO_EXISTE);
        }
        if (printerRepository.findByMacAddressCompact(context.compactMac()).isPresent()) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_MAC_EXISTE);
        }

        Seal seal = sealRepository.findById(context.seal().getId())
                .orElseThrow(() -> new FiscalizacionProtocolException(
                        FiscalizacionConstants.MSG_PRECINTO_NO_EXISTE));
        if (seal.getStatus() != SealStatus.DISPONIBLE || seal.getPrinter() != null) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_PRECINTO_ASIGNADO);
        }

        Long distributorId = context.distributorId();
        if (distributorId == null && sessionRegistry != null) {
            distributorId = sessionRegistry.resolvePendingDistributorId(context.ptrReg()).orElse(null);
        }

        Printer printer = new Printer();
        printer.setFiscalSerial(context.ptrReg());
        printer.setMacAddress(context.colonMac());
        printer.setVersionFirmware(context.firmwareVersion());
        printer.setModel(context.printerModel());
        printer.setPaid(false);
        printer.setDeviceType(DeviceType.INTERNO);
        printer.setEncryptionKey(
                PrinterEncryptionKeyGenerator.generateKey(context.ptrReg(), context.colonMac()));

        if (distributorId != null && distributorRepository != null) {
            Distributor distributor = distributorRepository.findById(distributorId)
                    .orElseThrow(() -> new FiscalizacionProtocolException(
                            "Distribuidor no encontrado para asignar la impresora."));
            printer.setDistributor(distributor);
            printer.setStatus(PrinterStatus.EN_CONSIGNACION);
        } else {
            printer.setStatus(PrinterStatus.SIN_ASIGNAR);
        }

        printer = printerRepository.save(printer);

        seal.setPrinter(printer);
        seal.setStatus(SealStatus.EN_IMPRESORA);
        seal.setInstallationDate(OffsetDateTime.now());
        sealRepository.save(seal);

        if (sessionRegistry != null) {
            sessionRegistry.clearPendingDistributor(context.ptrReg());
        }

        return printer;
    }
}
