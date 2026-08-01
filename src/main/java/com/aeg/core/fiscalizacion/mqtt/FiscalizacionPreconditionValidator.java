package com.aeg.core.fiscalizacion.mqtt;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.fiscalizacion.mqtt.dto.PtrFiscalizarMessage;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealColor;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FiscalizacionPreconditionValidator {

    private final PrinterRepository printerRepository;
    private final SealRepository sealRepository;
    private final PrinterModelRepository printerModelRepository;

    public FiscalizacionContext validate(PtrFiscalizarMessage message, String topicCompactMac) {
        String ptrReg = trim(message.ptrReg());
        String payloadMac = trim(message.macAddr());
        String precintoNro = trim(message.precintoNro());
        String precintoColorRaw = trim(message.precintoColor());
        String firmwareVersion = trim(message.firmwareVersion());
        String modelCode = trim(message.model());

        if (!StringUtils.hasText(ptrReg) || !StringUtils.hasText(payloadMac)
                || !StringUtils.hasText(precintoNro) || !StringUtils.hasText(modelCode)) {
            throw new FiscalizacionProtocolException("Invalid ptrFiscalizar payload: missing required fields");
        }

        String compactMac = MacAddressNormalizer.requireCompactForm(payloadMac);
        if (StringUtils.hasText(topicCompactMac) && !MacAddressNormalizer.sameMac(topicCompactMac, compactMac)) {
            throw new FiscalizacionProtocolException("MAC del topic no coincide con macAddr del payload");
        }
        String colonMac = MacAddressNormalizer.toColonForm(compactMac);

        if (printerRepository.existsByFiscalSerialIgnoreCase(ptrReg)) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_REGISTRO_EXISTE);
        }
        if (printerRepository.findByMacAddressCompact(compactMac).isPresent()) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_MAC_EXISTE);
        }

        Seal seal = sealRepository.findBySerialIgnoreCase(precintoNro).orElse(null);
        if (seal == null) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_PRECINTO_NO_EXISTE);
        }
        SealColor expectedColor = resolveSealColor(precintoColorRaw);
        if (expectedColor == null || seal.getColor() != expectedColor) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_PRECINTO_NO_EXISTE);
        }
        if (seal.getStatus() != SealStatus.DISPONIBLE || seal.getPrinter() != null) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_PRECINTO_ASIGNADO);
        }

        PrinterModel model = printerModelRepository.findFirstByModelCodeIgnoreCaseOrderByIdAsc(modelCode).orElse(null);
        if (model == null) {
            throw new FiscalizacionProtocolException(FiscalizacionConstants.MSG_MODELO_NO_EXISTE);
        }

        return new FiscalizacionContext(
                ptrReg.toUpperCase(Locale.ROOT),
                colonMac,
                compactMac,
                seal.getSerial(),
                seal.getColor().getValue(),
                firmwareVersion,
                model.getModelCode(),
                seal,
                model);
    }

    static SealColor resolveSealColor(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        SealColor direct = SealColor.fromValue(raw);
        if (direct != null) {
            return direct;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace('á', 'a')
                .replace('é', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ú', 'u')
                .replace(' ', '_');
        return SealColor.fromValue(normalized);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
