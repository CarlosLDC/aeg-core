package com.aeg.core.technician;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.technician.dto.TechnicianRequest;
import com.aeg.core.technician.dto.TechnicianResponse;

@Service
@Transactional
public class TechnicianServiceImpl implements TechnicianService {

	private final TechnicianRepository repository;
	private final EmployeeRepository employeeRepository;
	private final SecurityScopeService securityScope;

	public TechnicianServiceImpl(
			TechnicianRepository repository,
			EmployeeRepository employeeRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.employeeRepository = employeeRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TechnicianResponse> findAll() {
		if (securityScope.isAdmin()) {
			return repository.findAll().stream().map(this::toResponse).toList();
		}
		BranchScope scope = securityScope.resolveBranchScope();
		return switch (scope.visibility()) {
			case ALL -> repository.findAll().stream().map(this::toResponse).toList();
			case NONE -> List.of();
			case SCOPED -> repository.findByEmployee_Branch_IdIn(scope.branchIds()).stream()
					.map(this::toResponse)
					.toList();
		};
	}

	@Override
	@Transactional(readOnly = true)
	public TechnicianResponse findById(Long id) {
		Technician technician = findEntity(id);
		securityScope.assertBranchInScope(technician.getEmployee().getBranchId());
		return toResponse(technician);
	}

	@Override
	public TechnicianResponse create(TechnicianRequest request) {
		Technician e = new Technician();
		var employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));
		securityScope.assertBranchInScope(employee.getBranchId());
		e.setEmployee(employee);
		return toResponse(repository.save(e));
	}

	@Override
	public TechnicianResponse update(Long id, TechnicianRequest request) {
		Technician e = findEntity(id);
		securityScope.assertBranchInScope(e.getEmployee().getBranchId());
		var employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));
		securityScope.assertBranchInScope(employee.getBranchId());
		e.setEmployee(employee);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		Technician e = findEntity(id);
		securityScope.assertBranchInScope(e.getEmployee().getBranchId());
		repository.delete(e);
	}

	private Technician findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));
	}

	private TechnicianResponse toResponse(Technician e) {
		return new TechnicianResponse(e.getId(), e.getEmployeeId(), e.getCreatedAt());
	}
}
