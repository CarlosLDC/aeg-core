package com.aeg.core.distributorperson;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributorperson.dto.DistributorPersonRequest;
import com.aeg.core.distributorperson.dto.DistributorPersonResponse;
import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class DistributorPersonServiceImpl implements DistributorPersonService {

	private final DistributorPersonRepository repository;
	private final EmployeeRepository employeeRepository;

	public DistributorPersonServiceImpl(
			DistributorPersonRepository repository,
			EmployeeRepository employeeRepository) {
		this.repository = repository;
		this.employeeRepository = employeeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DistributorPersonResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DistributorPersonResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public DistributorPersonResponse create(DistributorPersonRequest request) {
		DistributorPerson e = new DistributorPerson();
		e.setEmployee(employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId())));
		return toResponse(repository.save(e));
	}

	@Override
	public DistributorPersonResponse update(Long id, DistributorPersonRequest request) {
		DistributorPerson e = findEntity(id);
		e.setEmployee(employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId())));
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private DistributorPerson findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Distributor person not found with id: " + id));
	}

	private DistributorPersonResponse toResponse(DistributorPerson e) {
		return new DistributorPersonResponse(e.getId(), e.getEmployeeId(), e.getCreatedAt());
	}
}
