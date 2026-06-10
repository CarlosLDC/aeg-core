package com.aeg.core.inspection;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.employee.Employee;
import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.inspection.dto.AnnualInspectionRequest;
import com.aeg.core.inspection.dto.AnnualInspectionResponse;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class AnnualInspectionServiceImpl implements AnnualInspectionService {

	private final AnnualInspectionRepository repository;
	private final PrinterRepository printerRepository;
	private final EmployeeRepository employeeRepository;
	private final SecurityScopeService securityScope;

	public AnnualInspectionServiceImpl(
			AnnualInspectionRepository repository,
			PrinterRepository printerRepository,
			EmployeeRepository employeeRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.printerRepository = printerRepository;
		this.employeeRepository = employeeRepository;
		this.securityScope = securityScope;
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
		if (securityScope.currentUser().getRole() == Role.DISTRIBUTOR) {
			return repository.findByPrinter_IdIn(printerIds).stream()
					.map(this::toResponse)
					.toList();
		}
		BranchScope scope = securityScope.resolveBranchScope();
		if (scope.visibility() != BranchScope.Visibility.SCOPED) {
			return List.of();
		}
		return repository
				.findByPrinter_IdInAndEmployee_Branch_IdIn(printerIds, scope.branchIds())
				.stream()
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
		AnnualInspection e = new AnnualInspection();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public AnnualInspectionResponse update(Long id, AnnualInspectionRequest request) {
		AnnualInspection e = findEntity(id);
		assertInspectionInScope(e);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		AnnualInspection inspection = findEntity(id);
		assertInspectionInScope(inspection);
		repository.delete(inspection);
	}

	private void assertInspectionInScope(AnnualInspection inspection) {
		Printer printer = inspection.getPrinter();
		Employee employee = inspection.getEmployee();
		if (printer != null) {
			securityScope.assertPrinterInScope(printer);
		}
		if (employee != null) {
			assertInspectionEmployeeInScope(employee);
		}
	}

	private void assertPrinterEligibleForInspection(Printer printer) {
		if (printer.getStatus() != PrinterStatus.ASIGNADA) {
			throw new IllegalArgumentException(
					"Only assigned printers can have annual inspections");
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
		Employee employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));
		assertInspectionEmployeeInScope(employee);
		e.setPrinter(printer);
		e.setEmployee(employee);
		e.setSealTampered(request.sealTampered());
		e.setNotes(request.notes());
		e.setPhotoUrls(request.photoUrls().toArray(String[]::new));
		if (request.inspectionDate() != null) {
			e.setInspectionDate(request.inspectionDate());
		}
	}

	private void assertInspectionEmployeeInScope(Employee employee) {
		if (securityScope.currentUser().getRole() == Role.DISTRIBUTOR) {
			securityScope.assertDistributorStaffBranch(employee.getBranchId());
			return;
		}
		securityScope.assertBranchInScope(employee.getBranchId());
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
