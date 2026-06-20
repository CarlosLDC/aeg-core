package com.aeg.core.technicalservice;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.security.UserRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.servicecenter.ServiceCenterRepository;
import com.aeg.core.technicalservice.dto.TechnicalServiceRequest;
import com.aeg.core.technicalservice.dto.TechnicalServiceResponse;

@Service
@Transactional
public class TechnicalServiceServiceImpl implements TechnicalServiceService {

	private final TechnicalServiceVisitRepository repository;
	private final PrinterRepository printerRepository;
	private final UserRepository userRepository;
	private final ServiceCenterRepository serviceCenterRepository;
	private final SealRepository sealRepository;
	private final DistributorRepository distributorRepository;
	private final SecurityScopeService securityScope;

	public TechnicalServiceServiceImpl(
			TechnicalServiceVisitRepository repository,
			PrinterRepository printerRepository,
			UserRepository userRepository,
			ServiceCenterRepository serviceCenterRepository,
			SealRepository sealRepository,
			DistributorRepository distributorRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.userRepository = userRepository;
		this.serviceCenterRepository = serviceCenterRepository;
		this.sealRepository = sealRepository;
		this.distributorRepository = distributorRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TechnicalServiceResponse> findAll() {
		if (securityScope.isGlobalReader()) {
			return repository.findAll().stream().map(this::toResponse).toList();
		}
		List<Long> printerIds = securityScope.visiblePrinterIds();
		if (printerIds.isEmpty()) {
			return List.of();
		}
		List<TechnicalServiceVisit> visits = repository.findByPrinter_IdIn(printerIds);
		User user = securityScope.currentUser();
		if (user.getRole() == Role.TECHNICIAN && user.getDistributorId() != null) {
			Long distributorId = user.getDistributorId();
			visits = visits.stream()
					.filter(v -> distributorId.equals(v.getDistributorId()) || v.getDistributorId() == null)
					.toList();
		}
		return visits.stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public TechnicalServiceResponse findById(Long id) {
		TechnicalServiceVisit visit = findEntity(id);
		assertVisitInScope(visit);
		return toResponse(visit);
	}

	@Override
	public TechnicalServiceResponse create(TechnicalServiceRequest request) {
		securityScope.assertCanWriteOperationalData();
		TechnicalServiceVisit e = new TechnicalServiceVisit();
		applyRequest(e, request);
		applySealStatusChanges(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public TechnicalServiceResponse update(Long id, TechnicalServiceRequest request) {
		securityScope.assertCanWriteOperationalData();
		TechnicalServiceVisit e = findEntity(id);
		assertVisitInScope(e);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		securityScope.assertCanWriteOperationalData();
		TechnicalServiceVisit visit = findEntity(id);
		assertVisitInScope(visit);
		repository.delete(visit);
	}

	private void assertVisitInScope(TechnicalServiceVisit visit) {
		Printer printer = visit.getPrinter();
		if (printer == null) {
			throw new ResourceNotFoundException("Printer missing on visit");
		}
		securityScope.assertPrinterInScope(printer);
	}

	private TechnicalServiceVisit findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Technical service not found with id: " + id));
	}

	private void applyRequest(TechnicalServiceVisit e, TechnicalServiceRequest r) {
		Printer printer = printerRepository.findById(r.printerId())
				.orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + r.printerId()));
		securityScope.assertPrinterInScope(printer);
		e.setPrinter(printer);

		User fieldUser = userRepository.findById(r.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + r.userId()));
		securityScope.assertFieldUserInScope(fieldUser);
		e.setReviewedByUser(fieldUser);

		if (r.serviceCenterId() != null) {
			var serviceCenter = serviceCenterRepository.findById(r.serviceCenterId())
					.orElseThrow(() -> new ResourceNotFoundException("Service center not found with id: " + r.serviceCenterId()));
			securityScope.assertBranchInScope(serviceCenter.getBranchId());
			e.setServiceCenter(serviceCenter);
		} else {
			e.setServiceCenter(null);
		}

		e.setSealTampered(r.sealTampered());
		e.setNotes(r.notes());
		e.setStartAt(r.startAt());
		e.setEndAt(r.endAt());
		e.setPhotoUrls(r.photoUrls().toArray(String[]::new));

		if (r.installedSealId() != null) {
			var seal = sealRepository.findById(r.installedSealId())
					.orElseThrow(() -> new ResourceNotFoundException("Installed seal not found with id: " + r.installedSealId()));
			securityScope.assertSealInScope(seal);
			e.setInstalledSeal(seal);
		} else {
			e.setInstalledSeal(null);
		}

		if (r.removedSealId() != null) {
			var seal = sealRepository.findById(r.removedSealId())
					.orElseThrow(() -> new ResourceNotFoundException("Removed seal not found with id: " + r.removedSealId()));
			securityScope.assertSealInScope(seal);
			e.setRemovedSeal(seal);
		} else {
			e.setRemovedSeal(null);
		}

		e.setInitialZReport(r.initialZReport());
		e.setFinalZReport(r.finalZReport());
		e.setCost(r.cost());
		e.setReportedFailure(r.reportedFailure());
		e.setRequestDate(r.requestDate());
		e.setInitialZDate(r.initialZDate());
		e.setFinalZDate(r.finalZDate());

		if (r.distributorId() != null) {
			e.setDistributor(distributorRepository.findById(r.distributorId())
					.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + r.distributorId())));
		} else {
			e.setDistributor(null);
		}
	}

	private void applySealStatusChanges(TechnicalServiceVisit visit, TechnicalServiceRequest request) {
		var endAt = request.endAt();
		Printer printer = visit.getPrinter();

		if (request.installedSealId() != null) {
			Seal installed = visit.getInstalledSeal();
			if (installed == null || installed.getStatus() != SealStatus.DISPONIBLE) {
				throw new IllegalArgumentException("installed seal is not available");
			}
			if (request.removedSealId() != null && request.installedSealId().equals(request.removedSealId())) {
				throw new IllegalArgumentException("installed and removed seal must differ");
			}
		}

		if (request.removedSealId() != null) {
			Seal removed = visit.getRemovedSeal();
			removed.setStatus(SealStatus.SUSTITUIDO);
			removed.setRemovalDate(endAt);
			removed.setPrinter(null);
			sealRepository.save(removed);
		}

		if (request.installedSealId() != null) {
			Seal installed = visit.getInstalledSeal();
			installed.setStatus(SealStatus.EN_IMPRESORA);
			installed.setInstallationDate(endAt);
			installed.setPrinter(printer);
			sealRepository.save(installed);
		}
	}

	private TechnicalServiceResponse toResponse(TechnicalServiceVisit e) {
		return new TechnicalServiceResponse(
				e.getId(),
				e.getPrinterId(),
				e.getUserId(),
				e.getServiceCenterId(),
				e.getSealTampered(),
				e.getNotes(),
				e.getStartAt(),
				e.getCreatedAt(),
				e.getEndAt(),
				e.getPhotoUrls() == null ? List.of() : Arrays.asList(e.getPhotoUrls()),
				e.getInstalledSealId(),
				e.getRemovedSealId(),
				e.getInitialZReport(),
				e.getFinalZReport(),
				e.getCost(),
				e.getReportedFailure(),
				e.getRequestDate(),
				e.getInitialZDate(),
				e.getFinalZDate(),
				e.getDistributorId());
	}
}
