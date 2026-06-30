package com.aeg.core.inspection.qr;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Component
public class AnnualInspectionQrValidator {

    private final PrinterRepository printerRepository;
    private final AnnualInspectionQrDecoder decoder;

    public AnnualInspectionQrValidator(
            PrinterRepository printerRepository,
            AnnualInspectionQrDecoder decoder) {
        this.printerRepository = printerRepository;
        this.decoder = decoder;
    }

    public AnnualInspectionQrPayload decodeAndValidate(
            Long printerId,
            String qrCodigo,
            String registroImpresora) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));

        AnnualInspectionQrPayload payload = decoder.decode(qrCodigo);
        validateAgainstPrinter(printer, payload, registroImpresora);
        return payload;
    }

    public void validateAgainstPrinter(
            Printer printer,
            AnnualInspectionQrPayload payload,
            String registroImpresora) {
        String expectedRegistro = normalizeRegistro(registroImpresora);
        if (expectedRegistro == null) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }
        if (!expectedRegistro.equalsIgnoreCase(payload.registro().trim())) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }
        if (!MacAddressNormalizer.sameMac(printer.getMacAddress(), payload.mac())) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }
        if (payload.fecha() == null || payload.fecha().isBlank()) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }
    }

    private static String normalizeRegistro(String registroImpresora) {
        if (registroImpresora == null) {
            return null;
        }
        String trimmed = registroImpresora.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
