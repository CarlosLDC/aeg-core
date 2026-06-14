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
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new EnajenacionProtocolException("Printer not found for completion"));
        if (printer.getStatus() == PrinterStatus.ENAJENADA) {
            return;
        }
        if (printer.getStatus() != PrinterStatus.ASIGNADA) {
            throw new EnajenacionProtocolException("Cannot complete enajenacion from status " + printer.getStatus());
        }
        printer.setStatus(PrinterStatus.ENAJENADA);
        if (printer.getInstallationDate() == null) {
            printer.setInstallationDate(OffsetDateTime.now());
        }
        printerRepository.save(printer);
    }
}
