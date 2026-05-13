package com.aeg.core.serviciotecnico;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.servicecenter.ServiceCenterRepository;
import com.aeg.core.serviciotecnico.dto.ServicioTecnicoRequest;
import com.aeg.core.serviciotecnico.dto.ServicioTecnicoResponse;
import com.aeg.core.tecnico.TecnicoRepository;

@Service
@Transactional
public class ServicioTecnicoServiceImpl implements ServicioTecnicoService {

	private final ServicioTecnicoRepository repository;
	private final PrinterRepository printerRepository;
	private final TecnicoRepository tecnicoRepository;
	private final ServiceCenterRepository serviceCenterRepository;
	private final SealRepository sealRepository;
	private final DistributorRepository distributorRepository;

	public ServicioTecnicoServiceImpl(
			ServicioTecnicoRepository repository,
			PrinterRepository printerRepository,
			TecnicoRepository tecnicoRepository,
			ServiceCenterRepository serviceCenterRepository,
			SealRepository sealRepository,
			DistributorRepository distributorRepository) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.tecnicoRepository = tecnicoRepository;
		this.serviceCenterRepository = serviceCenterRepository;
		this.sealRepository = sealRepository;
		this.distributorRepository = distributorRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ServicioTecnicoResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ServicioTecnicoResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public ServicioTecnicoResponse create(ServicioTecnicoRequest request) {
		ServicioTecnico e = new ServicioTecnico();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public ServicioTecnicoResponse update(Long id, ServicioTecnicoRequest request) {
		ServicioTecnico e = findEntity(id);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private ServicioTecnico findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Technical service not found with id: " + id));
	}

	private void applyRequest(ServicioTecnico e, ServicioTecnicoRequest r) {
		e.setPrinter(printerRepository.getReferenceById(r.printerId()));
		e.setTechnician(tecnicoRepository.getReferenceById(r.technicianId()));
		if (r.serviceCenterId() != null) {
			e.setServiceCenter(serviceCenterRepository.getReferenceById(r.serviceCenterId()));
		} else {
			e.setServiceCenter(null);
		}
		e.setSealTampered(r.sealTampered());
		e.setNotes(r.notes());
		e.setStartAt(r.startAt());
		e.setEndAt(r.endAt());
		e.setPhotoUrls(r.photoUrls().toArray(String[]::new));
		if (r.installedSealId() != null) {
			e.setInstalledSeal(sealRepository.getReferenceById(r.installedSealId()));
		} else {
			e.setInstalledSeal(null);
		}
		if (r.removedSealId() != null) {
			e.setRemovedSeal(sealRepository.getReferenceById(r.removedSealId()));
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
			e.setDistributor(distributorRepository.getReferenceById(r.distributorId()));
		} else {
			e.setDistributor(null);
		}
	}

	private ServicioTecnicoResponse toResponse(ServicioTecnico e) {
		return new ServicioTecnicoResponse(
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
