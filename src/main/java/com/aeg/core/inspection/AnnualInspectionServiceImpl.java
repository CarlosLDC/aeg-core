package com.aeg.core.inspection;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.inspection.dto.AnnualInspectionRequest;
import com.aeg.core.inspection.dto.AnnualInspectionResponse;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class AnnualInspectionServiceImpl implements AnnualInspectionService {

	private final AnnualInspectionRepository repository;
	private final PrinterRepository printerRepository;
	private final EmployeeRepository employeeRepository;

	public AnnualInspectionServiceImpl(
			AnnualInspectionRepository repository,
			PrinterRepository printerRepository,
			EmployeeRepository employeeRepository) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.employeeRepository = employeeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AnnualInspectionResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AnnualInspectionResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public AnnualInspectionResponse create(AnnualInspectionRequest request) {
		AnnualInspection e = new AnnualInspection();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public AnnualInspectionResponse update(Long id, AnnualInspectionRequest request) {
		AnnualInspection e = findEntity(id);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private AnnualInspection findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Annual inspection not found with id: " + id));
	}

	private void applyRequest(AnnualInspection e, AnnualInspectionRequest request) {
		e.setPrinter(printerRepository.findById(request.printerId())
				.orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + request.printerId())));
		e.setEmployee(employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId())));
		e.setSealTampered(request.sealTampered());
		e.setNotes(request.notes());
		e.setPhotoUrls(request.photoUrls().toArray(String[]::new));
		if (request.inspectionDate() != null) {
			e.setInspectionDate(request.inspectionDate());
		}
	}

	private AnnualInspectionResponse toResponse(AnnualInspection e) {
		return new AnnualInspectionResponse(
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
