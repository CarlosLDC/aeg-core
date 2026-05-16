package com.aeg.core.employee;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.BranchRepository;
import com.aeg.core.employee.dto.EmployeeRequest;
import com.aeg.core.employee.dto.EmployeeResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

	private final EmployeeRepository repository;
	private final BranchRepository branchRepository;

	public EmployeeServiceImpl(EmployeeRepository repository, BranchRepository branchRepository) {
		this.repository = repository;
		this.branchRepository = branchRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmployeeResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public EmployeeResponse findById(Long id) {
		return toResponse(findEntity(id));
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
		if (!e.getNationalId().equalsIgnoreCase(request.nationalId())
				&& repository.existsByNationalIdIgnoreCase(request.nationalId())) {
			throw new IllegalArgumentException("nationalId already exists: " + request.nationalId());
		}
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private Employee findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
	}

	private void applyRequest(Employee e, EmployeeRequest request) {
		e.setNationalId(request.nationalId());
		e.setName(request.name());
		e.setPhone(request.phone());
		e.setEmail(request.email());
		e.setType(request.type());
		e.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
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
