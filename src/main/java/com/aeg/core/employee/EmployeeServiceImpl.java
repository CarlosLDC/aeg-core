package com.aeg.core.employee;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.BranchRepository;
import com.aeg.core.employee.dto.EmployeeRequest;
import com.aeg.core.employee.dto.EmployeeResponse;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

	private final EmployeeRepository repository;
	private final BranchRepository branchRepository;
	private final SecurityScopeService securityScope;

	public EmployeeServiceImpl(
			EmployeeRepository repository,
			BranchRepository branchRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.branchRepository = branchRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmployeeResponse> findAll() {
		if (securityScope.isAdmin()) {
			return repository.findAll().stream().map(this::toResponse).toList();
		}
		if (securityScope.currentUser().getRole() == com.aeg.core.security.Role.DISTRIBUTOR) {
			var staffBranches = securityScope.resolveDistributorStaffBranchIds();
			if (staffBranches.isEmpty()) {
				return List.of();
			}
			return repository.findByBranch_IdIn(staffBranches).stream().map(this::toResponse).toList();
		}
		BranchScope scope = securityScope.resolveBranchScope();
		return switch (scope.visibility()) {
			case ALL -> repository.findAll().stream().map(this::toResponse).toList();
			case NONE -> List.of();
			case SCOPED -> repository.findByBranch_IdIn(scope.branchIds()).stream().map(this::toResponse).toList();
		};
	}

	@Override
	@Transactional(readOnly = true)
	public EmployeeResponse findById(Long id) {
		Employee employee = findEntity(id);
		securityScope.assertDistributorStaffBranch(employee.getBranchId());
		return toResponse(employee);
	}

	@Override
	public EmployeeResponse create(EmployeeRequest request) {
		if (repository.existsByNationalIdIgnoreCase(request.nationalId())) {
			throw new IllegalArgumentException("nationalId already exists: " + request.nationalId());
		}
		Employee e = new Employee();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public EmployeeResponse update(Long id, EmployeeRequest request) {
		Employee e = findEntity(id);
		securityScope.assertDistributorStaffBranch(e.getBranchId());
		if (!e.getNationalId().equalsIgnoreCase(request.nationalId())
				&& repository.existsByNationalIdIgnoreCase(request.nationalId())) {
			throw new IllegalArgumentException("nationalId already exists: " + request.nationalId());
		}
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		Employee e = findEntity(id);
		securityScope.assertDistributorStaffBranch(e.getBranchId());
		repository.delete(e);
	}

	private Employee findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
	}

	private void applyRequest(Employee e, EmployeeRequest request) {
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertDistributorStaffBranch(branch.getId());
		e.setNationalId(request.nationalId());
		e.setName(request.name());
		e.setPhone(request.phone());
		e.setEmail(request.email());
		e.setType(request.type());
		e.setBranch(branch);
	}

	private EmployeeResponse toResponse(Employee e) {
		return new EmployeeResponse(
				e.getId(),
				e.getNationalId(),
				e.getName(),
				e.getPhone(),
				e.getEmail(),
				e.getCreatedAt(),
				e.getType(),
				e.getBranchId());
	}
}
