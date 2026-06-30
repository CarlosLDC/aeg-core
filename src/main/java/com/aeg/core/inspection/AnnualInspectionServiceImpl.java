package com.aeg.core.inspection;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.inspection.dto.AnnualInspectionRequest;
import com.aeg.core.inspection.dto.AnnualInspectionResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.organization.OrgCapability;
import com.aeg.core.organization.OrganizationCapabilityService;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.security.UserRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class AnnualInspectionServiceImpl implements AnnualInspectionService {

	private final AnnualInspectionRepository repository;
	private final PrinterRepository printerRepository;
	private final UserRepository userRepository;
	private final SecurityScopeService securityScope;
	private final OrganizationCapabilityService organizationCapability;

	public AnnualInspectionServiceImpl(
			AnnualInspectionRepository repository,
			PrinterRepository printerRepository,
			UserRepository userRepository,
			SecurityScopeService securityScope,
			OrganizationCapabilityService organizationCapability) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.userRepository = userRepository;
		this.securityScope = securityScope;
		this.organizationCapability = organizationCapability;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AnnualInspectionResponse> findAll() {
		if (securityScope.isGlobalReader()) {
			return repository.findAll().stream().map(this::toResponse).toList();
		}
		List<Long> printerIds = securityScope.visiblePrinterIds();
		if (printerIds.isEmpty()) {
			return List.of();
		}
		return repository.findByPrinter_IdIn(printerIds).stream()
				.map(this::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AnnualInspectionResponse findById(Long id) {
		AnnualInspection inspection = findEntity(id);
		assertInspectionInScope(inspection);
		return toResponse(inspection);
	}

	@Override
	public AnnualInspectionResponse create(AnnualInspectionRequest request) {
		securityScope.assertCanWriteAnnualInspection();
		organizationCapability.assertActorCan(OrgCapability.WRITE_ANNUAL_INSPECTION);
		AnnualInspection e = new AnnualInspection();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public AnnualInspectionResponse update(Long id, AnnualInspectionRequest request) {
		securityScope.assertCanWriteAnnualInspection();
		organizationCapability.assertActorCan(OrgCapability.WRITE_ANNUAL_INSPECTION);
		AnnualInspection e = findEntity(id);
		assertInspectionInScope(e);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		securityScope.assertCanWriteAnnualInspection();
		AnnualInspection inspection = findEntity(id);
		assertInspectionInScope(inspection);
		repository.delete(inspection);
	}

	private void assertInspectionInScope(AnnualInspection inspection) {
		Printer printer = inspection.getPrinter();
		if (printer != null) {
			securityScope.assertPrinterInScope(printer);
		}
	}

	private void assertPrinterEligibleForInspection(Printer printer) {
		if (printer.getStatus() != PrinterStatus.ASIGNADA
				&& printer.getStatus() != PrinterStatus.ENAJENADA) {
			throw new IllegalArgumentException(
					"Only assigned or enajenada printers can have annual inspections");
		}
	}

	private AnnualInspection findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Annual inspection not found with id: " + id));
	}

	private void applyRequest(AnnualInspection e, AnnualInspectionRequest request) {
		Printer printer = printerRepository.findById(request.printerId())
				.orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId()));
		securityScope.assertPrinterInScope(printer);
		assertPrinterEligibleForInspection(printer);

		User fieldUser = userRepository.findById(request.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));
		securityScope.assertInspectionInspectorInScope(fieldUser);

		e.setPrinter(printer);
		e.setInspectorUser(fieldUser);
		applyChecklistAndSealTampered(e, request);
		e.setNotes(request.notes());
		e.setPhotoUrls(request.photoUrls().toArray(String[]::new));
		if (request.inspectionDate() != null) {
			e.setInspectionDate(request.inspectionDate());
		}
		e.setMqttRegistroImpresora(normalizeOptionalText(request.mqttRegistroImpresora()));
		e.setMqttSetDateRevOAt(request.mqttSetDateRevOAt());
		e.setMqttNumeroFacturaPrueba(request.mqttNumeroFacturaPrueba());
	}

	private void applyChecklistAndSealTampered(AnnualInspection e, AnnualInspectionRequest request) {
		boolean hasChecklist = request.chkPrecinto() != null
				|| request.chkEtiquetaFiscal() != null
				|| request.chkFactura() != null
				|| request.chkNotaCredito() != null
				|| request.chkSensorPapel() != null;

		if (hasChecklist) {
			if (request.chkPrecinto() != null) {
				e.setChkPrecinto(request.chkPrecinto());
				e.setSealTampered(!request.chkPrecinto());
			} else if (request.sealTampered() != null) {
				e.setSealTampered(request.sealTampered());
				e.setChkPrecinto(!request.sealTampered());
			}
			if (request.chkEtiquetaFiscal() != null) {
				e.setChkEtiquetaFiscal(request.chkEtiquetaFiscal());
			}
			if (request.chkFactura() != null) {
				e.setChkFactura(request.chkFactura());
			}
			if (request.chkNotaCredito() != null) {
				e.setChkNotaCredito(request.chkNotaCredito());
			}
			if (request.chkSensorPapel() != null) {
				e.setChkSensorPapel(request.chkSensorPapel());
			}
			return;
		}

		e.setSealTampered(request.sealTampered());
		if (request.sealTampered() != null) {
			e.setChkPrecinto(!request.sealTampered());
		}
	}

	private static Boolean effectiveChkPrecinto(AnnualInspection e) {
		if (e.getChkPrecinto() != null) {
			return e.getChkPrecinto();
		}
		if (e.getSealTampered() != null) {
			return !e.getSealTampered();
		}
		return null;
	}

	private static Boolean effectiveSealTampered(AnnualInspection e) {
		if (e.getSealTampered() != null) {
			return e.getSealTampered();
		}
		Boolean chkPrecinto = e.getChkPrecinto();
		return chkPrecinto != null ? !chkPrecinto : null;
	}

	private static String normalizeOptionalText(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private AnnualInspectionResponse toResponse(AnnualInspection e) {
		return new AnnualInspectionResponse(
				e.getId(),
				e.getPrinterId(),
				e.getUserId(),
				effectiveSealTampered(e),
				e.getNotes(),
				e.getCreatedAt(),
				e.getPhotoUrls() == null ? List.of() : Arrays.asList(e.getPhotoUrls()),
				e.getInspectionDate(),
				e.getMqttRegistroImpresora(),
				e.getMqttSetDateRevOAt(),
				e.getMqttNumeroFacturaPrueba(),
				effectiveChkPrecinto(e),
				e.getChkEtiquetaFiscal(),
				e.getChkFactura(),
				e.getChkNotaCredito(),
				e.getChkSensorPapel(),
				e.getMqttQrCodigo(),
				e.getMqttQrRegistro(),
				e.getMqttQrMac(),
				e.getMqttQrFecha());
	}
}
