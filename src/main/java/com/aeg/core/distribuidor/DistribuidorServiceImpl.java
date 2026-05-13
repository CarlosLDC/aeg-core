package com.aeg.core.distribuidor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distribuidor.dto.DistribuidorRequest;
import com.aeg.core.distribuidor.dto.DistribuidorResponse;
import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class DistribuidorServiceImpl implements DistribuidorService {

	private final DistribuidorRepository repository;
	private final EmployeeRepository employeeRepository;

	public DistribuidorServiceImpl(DistribuidorRepository repository, EmployeeRepository employeeRepository) {
		this.repository = repository;
		this.employeeRepository = employeeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DistribuidorResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DistribuidorResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public DistribuidorResponse create(DistribuidorRequest request) {
		Distribuidor e = new Distribuidor();
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		return toResponse(repository.save(e));
	}

	@Override
	public DistribuidorResponse update(Long id, DistribuidorRequest request) {
		Distribuidor e = findEntity(id);
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private Distribuidor findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Distribuidor not found with id: " + id));
	}

	private DistribuidorResponse toResponse(Distribuidor e) {
		return new DistribuidorResponse(e.getId(), e.getEmployeeId(), e.getCreatedAt());
	}
}
