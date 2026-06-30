package com.aeg.core.inspection.qr;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.fiscalbook.dto.FiscalBookLookupInspectionByQrResponse;
import com.aeg.core.inspection.AnnualInspection;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AnnualInspectionQrLookupService {

	private static final String NOT_FOUND_MESSAGE = AnnualInspectionQrMessages.INVALID_CODE;

	private final AnnualInspectionQrDecoder decoder;
	private final AnnualInspectionRepository inspectionRepository;
	private final SecurityScopeService securityScope;

	public AnnualInspectionQrLookupService(
			AnnualInspectionQrDecoder decoder,
			AnnualInspectionRepository inspectionRepository,
			SecurityScopeService securityScope) {
		this.decoder = decoder;
		this.inspectionRepository = inspectionRepository;
		this.securityScope = securityScope;
	}

	public FiscalBookLookupInspectionByQrResponse lookup(String qrCodigo) {
		AnnualInspectionQrPayload payload = decoder.decode(qrCodigo);
		Printer printer = findVisiblePrinterByMac(payload.mac());

		Optional<AnnualInspection> exactQrMatch = findExactQrMatch(printer.getId(), qrCodigo);
		if (exactQrMatch.isPresent()) {
			return toResponse(exactQrMatch.get(), printer, payload);
		}

		List<AnnualInspection> registroMatches = inspectionRepository
				.findByPrinter_IdAndMqttRegistroImpresoraIgnoreCase(
						printer.getId(),
						payload.registro().trim());

		AnnualInspection match = registroMatches.stream()
				.filter(inspection -> macCoherent(inspection, printer, payload.mac()))
				.sorted(disambiguationComparator(payload.fecha()))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));

		return toResponse(match, printer, payload);
	}

	private Printer findVisiblePrinterByMac(String qrMac) {
		return securityScope.findVisiblePrinters().stream()
				.filter(printer -> MacAddressNormalizer.sameMac(printer.getMacAddress(), qrMac))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(AnnualInspectionQrMessages.INVALID_CODE));
	}

	private Optional<AnnualInspection> findExactQrMatch(Long printerId, String qrCodigo) {
		String trimmed = qrCodigo == null ? "" : qrCodigo.trim();
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}

		Optional<AnnualInspection> exactMatch = inspectionRepository
				.findByPrinter_IdAndMqttQrCodigo(printerId, trimmed);
		if (exactMatch.isPresent()) {
			return exactMatch;
		}

		String normalized = decoder.normalizeQrCodigo(trimmed);
		if (normalized.isEmpty() || normalized.equals(trimmed)) {
			return Optional.empty();
		}

		return inspectionRepository.findByPrinter_IdAndMqttQrCodigo(printerId, normalized);
	}

	private static boolean macCoherent(
			AnnualInspection inspection,
			Printer printer,
			String qrMac) {
		if (inspection.getMqttQrMac() != null && !inspection.getMqttQrMac().isBlank()) {
			return MacAddressNormalizer.sameMac(inspection.getMqttQrMac(), qrMac);
		}
		return MacAddressNormalizer.sameMac(printer.getMacAddress(), qrMac);
	}

	private static Comparator<AnnualInspection> disambiguationComparator(String qrFecha) {
		String fecha = qrFecha == null ? "" : qrFecha.trim();
		return Comparator
				.<AnnualInspection>comparingInt(inspection -> fechaMatchScore(inspection, fecha))
				.reversed()
				.thenComparing(
						AnnualInspection::getCreatedAt,
						Comparator.nullsLast(Comparator.reverseOrder()));
	}

	private static int fechaMatchScore(AnnualInspection inspection, String qrFecha) {
		if (qrFecha.isEmpty()) {
			return 0;
		}
		String stored = inspection.getMqttQrFecha();
		if (stored != null && stored.trim().equalsIgnoreCase(qrFecha)) {
			return 2;
		}
		return 0;
	}

	private static FiscalBookLookupInspectionByQrResponse toResponse(
			AnnualInspection inspection,
			Printer printer,
			AnnualInspectionQrPayload payload) {
		return new FiscalBookLookupInspectionByQrResponse(
				Objects.requireNonNull(inspection.getId()),
				Objects.requireNonNull(printer.getId()),
				printer.getFiscalSerial(),
				payload.registro(),
				payload.mac(),
				payload.fecha());
	}
}
