package com.aeg.core.technicalservice;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.servicecenter.ServiceCenterRepository;
import com.aeg.core.technicalservice.dto.TechnicalServiceRequest;
import com.aeg.core.technicalservice.dto.TechnicalServiceResponse;
import com.aeg.core.technician.TechnicianRepository;

@Service
@Transactional
public class TechnicalServiceServiceImpl implements TechnicalServiceService {

	private final TechnicalServiceVisitRepository repository;
	private final PrinterRepository printerRepository;
	private final TechnicianRepository technicianRepository;
	private final ServiceCenterRepository serviceCenterRepository;
	private final SealRepository sealRepository;
	private final DistributorRepository distributorRepository;

	public TechnicalServiceServiceImpl(
			TechnicalServiceVisitRepository repository,
			PrinterRepository printerRepository,
			TechnicianRepository technicianRepository,
			ServiceCenterRepository serviceCenterRepository,
			SealRepository sealRepository,
			DistributorRepository distributorRepository) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.technicianRepository = technicianRepository;
		this.serviceCenterRepository = serviceCenterRepository;
		this.sealRepository = sealRepository;
		this.distributorRepository = distributorRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TechnicalServiceResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public TechnicalServiceResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public TechnicalServiceResponse create(TechnicalServiceRequest request) {
		TechnicalServiceVisit e = new TechnicalServiceVisit();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public TechnicalServiceResponse update(Long id, TechnicalServiceRequest request) {
		TechnicalServiceVisit e = findEntity(id);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private TechnicalServiceVisit findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Technical service not found with id: " + id));
	}

	private void applyRequest(TechnicalServiceVisit e, TechnicalServiceRequest r) {
		e.setPrinter(printerRepository.findById(r.printerId())
				.orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + r.printerId())));
		e.setTechnician(technicianRepository.findById(r.technicianId())
				.orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + r.technicianId())));
		
		if (r.serviceCenterId() != null) {
			e.setServiceCenter(serviceCenterRepository.findById(r.serviceCenterId())
					.orElseThrow(() -> new ResourceNotFoundException("Service center not found with id: " + r.serviceCenterId())));
		} else {
			e.setServiceCenter(null);
		}
		
		e.setSealTampered(r.sealTampered());
		e.setNotes(r.notes());
		e.setStartAt(r.startAt());
		e.setEndAt(r.endAt());
		e.setPhotoUrls(r.photoUrls().toArray(String[]::new));
		
		if (r.installedSealId() != null) {
			e.setInstalledSeal(sealRepository.findById(r.installedSealId())
					.orElseThrow(() -> new ResourceNotFoundException("Installed seal not found with id: " + r.installedSealId())));
		} else {
			e.setInstalledSeal(null);
		}
		
		if (r.removedSealId() != null) {
			e.setRemovedSeal(sealRepository.findById(r.removedSealId())
					.orElseThrow(() -> new ResourceNotFoundException("Removed seal not found with id: " + r.removedSealId())));
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

	private TechnicalServiceResponse toResponse(TechnicalServiceVisit e) {
		return new TechnicalServiceResponse(
				e.getId(),
				e.getPrinterId(),
				e.getTechnicianId(),
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
