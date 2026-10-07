package com.aeg.core.enajenacion.mqtt;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnajenacionCompletionService {

    private final PrinterRepository printerRepository;

    @Transactional
    public void markEnajenada(Long printerId) {
        markEnajenada(printerId, null);
    }

    @Transactional
    public void markEnajenada(Long printerId, String encryptionKey) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new EnajenacionProtocolException("Printer not found for completion"));
        if (printer.getStatus() == PrinterStatus.ENAJENADA) {
            if (encryptionKey != null && !encryptionKey.isBlank()) {
                printer.setEncryptionKey(encryptionKey);
                printerRepository.save(printer);
            } else if (printer.getEncryptionKey() == null) {
                String gen = com.aeg.core.printer.PrinterEncryptionKeyGenerator.generateKey(
                        printer.getFiscalSerial(), printer.getMacAddress());
                if (gen != null) {
                    printer.setEncryptionKey(gen);
                    printerRepository.save(printer);
                }
            }
            return;
        }
        if (!printer.getStatus().isEligibleForMqttEnajenacion()) {
            throw new EnajenacionProtocolException("Cannot complete enajenacion from status " + printer.getStatus());
        }
        printer.setStatus(PrinterStatus.ENAJENADA);
        if (printer.getInstallationDate() == null) {
            printer.setInstallationDate(OffsetDateTime.now());
        }
        if (encryptionKey != null && !encryptionKey.isBlank()) {
            printer.setEncryptionKey(encryptionKey);
        } else if (printer.getEncryptionKey() == null) {
            printer.setEncryptionKey(com.aeg.core.printer.PrinterEncryptionKeyGenerator.generateKey(
                    printer.getFiscalSerial(), printer.getMacAddress()));
        }
        printerRepository.save(printer);
    }
}
