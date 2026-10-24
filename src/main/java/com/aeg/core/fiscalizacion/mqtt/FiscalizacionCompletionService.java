package com.aeg.core.fiscalizacion.mqtt;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FiscalizacionCompletionService {

    private final PrinterRepository printerRepository;
    private final SealRepository sealRepository;

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

        Printer printer = new Printer();
        printer.setFiscalSerial(context.ptrReg());
        printer.setMacAddress(context.colonMac());
        printer.setVersionFirmware(context.firmwareVersion());
        printer.setModel(context.printerModel());
        printer.setStatus(PrinterStatus.SIN_ASIGNAR);
        printer.setPaid(false);
        printer.setDeviceType(DeviceType.INTERNO);
        printer = printerRepository.save(printer);

        seal.setPrinter(printer);
        seal.setStatus(SealStatus.EN_IMPRESORA);
        seal.setInstallationDate(OffsetDateTime.now());
        sealRepository.save(seal);

        return printer;
    }
}
