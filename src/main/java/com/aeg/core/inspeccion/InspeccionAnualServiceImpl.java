package com.aeg.core.inspeccion;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.inspeccion.dto.InspeccionAnualRequest;
import com.aeg.core.inspeccion.dto.InspeccionAnualResponse;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class InspeccionAnualServiceImpl implements InspeccionAnualService {

	private final InspeccionAnualRepository repository;
	private final PrinterRepository printerRepository;
	private final EmployeeRepository employeeRepository;

	public InspeccionAnualServiceImpl(
			InspeccionAnualRepository repository,
			PrinterRepository printerRepository,
			EmployeeRepository employeeRepository) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.employeeRepository = employeeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<InspeccionAnualResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public InspeccionAnualResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public InspeccionAnualResponse create(InspeccionAnualRequest request) {
		InspeccionAnual e = new InspeccionAnual();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public InspeccionAnualResponse update(Long id, InspeccionAnualRequest request) {
		InspeccionAnual e = findEntity(id);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private InspeccionAnual findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Annual inspection not found with id: " + id));
	}

	private void applyRequest(InspeccionAnual e, InspeccionAnualRequest request) {
		e.setPrinter(printerRepository.getReferenceById(request.printerId()));
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		e.setSealTampered(request.sealTampered());
		e.setNotes(request.notes());
		e.setPhotoUrls(request.photoUrls().toArray(String[]::new));
		if (request.inspectionDate() != null) {
			e.setInspectionDate(request.inspectionDate());
		}
	}

	private InspeccionAnualResponse toResponse(InspeccionAnual e) {
		return new InspeccionAnualResponse(
				e.getId(),
				e.getPrinterId(),
				e.getEmployeeId(),
				e.getSealTampered(),
				e.getNotes(),
				e.getCreatedAt(),
				e.getPhotoUrls() == null ? List.of() : Arrays.asList(e.getPhotoUrls()),
				e.getInspectionDate());
	}
}
