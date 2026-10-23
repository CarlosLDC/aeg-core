package com.aeg.core.fiscalizacion.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class FiscalizacionPreconditionValidator {

    private final PrinterRepository printerRepository;
    private final SealRepository sealRepository;
    private final PrinterModelRepository printerModelRepository;
    private final FiscalizacionSessionRegistry sessionRegistry;

    public FiscalizacionPreconditionValidator(
            PrinterRepository printerRepository,
            SealRepository sealRepository,
            PrinterModelRepository printerModelRepository) {
        this(printerRepository, sealRepository, printerModelRepository, null);
    }

    @Autowired
    public FiscalizacionPreconditionValidator(
            PrinterRepository printerRepository,
            SealRepository sealRepository,
            PrinterModelRepository printerModelRepository,
            FiscalizacionSessionRegistry sessionRegistry) {
        this.printerRepository = printerRepository;
        this.sealRepository = sealRepository;
        this.printerModelRepository = printerModelRepository;
        this.sessionRegistry = sessionRegistry;
    }

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

        String normalizedPtrReg = ptrReg.toUpperCase(Locale.ROOT);
        Long distributorId = sessionRegistry == null
                ? null
                : sessionRegistry.resolvePendingDistributorId(normalizedPtrReg).orElse(null);

        return new FiscalizacionContext(
                normalizedPtrReg,
                colonMac,
                compactMac,
                seal.getSerial(),
                seal.getColor().getValue(),
                firmwareVersion,
                model.getModelCode(),
                seal,
                model,
                distributorId);
    }

    /**
     * Verificación previa (solo lectura) para el asistente de fiscalización por USB: aplica las mismas
     * reglas que {@link #validate} antes de enviar {@code ptrFiscalizarRemoto}, que es irreversible.
     * El modelo no se puede verificar aquí porque lo reporta el firmware en {@code ptrFiscalizar}.
     *
     * @param mac opcional; si viene se valida formato y unicidad.
     * @return lista de mensajes de error (vacía si todo es válido).
     */
    public List<String> precheck(String ptrRegRaw, String precintoNroRaw, String precintoColorRaw, String macRaw) {
        List<String> errors = new ArrayList<>();
        String ptrReg = trim(ptrRegRaw);
        String precintoNro = trim(precintoNroRaw);
        String precintoColor = trim(precintoColorRaw);
        String mac = trim(macRaw);

        if (!StringUtils.hasText(ptrReg)) {
            errors.add("El serial (registro) de la impresora es obligatorio.");
        } else if (printerRepository.existsByFiscalSerialIgnoreCase(ptrReg)) {
            errors.add(FiscalizacionConstants.MSG_REGISTRO_EXISTE);
        }

        if (StringUtils.hasText(mac)) {
            try {
                String compactMac = MacAddressNormalizer.requireCompactForm(mac);
                if (printerRepository.findByMacAddressCompact(compactMac).isPresent()) {
                    errors.add(FiscalizacionConstants.MSG_MAC_EXISTE);
                }
            } catch (IllegalArgumentException ex) {
                errors.add("La dirección MAC no tiene un formato válido.");
            }
        }

        if (!StringUtils.hasText(precintoNro)) {
            errors.add("El número de precinto es obligatorio.");
            return errors;
        }
        Seal seal = sealRepository.findBySerialIgnoreCase(precintoNro).orElse(null);
        if (seal == null) {
            errors.add(FiscalizacionConstants.MSG_PRECINTO_NO_EXISTE);
            return errors;
        }
        SealColor expectedColor = resolveSealColor(precintoColor);
        if (expectedColor == null || seal.getColor() != expectedColor) {
            errors.add("El color del precinto no coincide con el registrado.");
        }
        if (seal.getStatus() != SealStatus.DISPONIBLE || seal.getPrinter() != null) {
            errors.add(FiscalizacionConstants.MSG_PRECINTO_ASIGNADO);
        }
        return errors;
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
