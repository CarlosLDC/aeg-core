package com.aeg.core.technician;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.technician.dto.TechnicianRequest;
import com.aeg.core.technician.dto.TechnicianResponse;

@Service
@Transactional
public class TechnicianServiceImpl implements TechnicianService {

	private final TechnicianRepository repository;
	private final EmployeeRepository employeeRepository;

	public TechnicianServiceImpl(TechnicianRepository repository, EmployeeRepository employeeRepository) {
		this.repository = repository;
		this.employeeRepository = employeeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TechnicianResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public TechnicianResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public TechnicianResponse create(TechnicianRequest request) {
		Technician e = new Technician();
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		return toResponse(repository.save(e));
	}

	@Override
	public TechnicianResponse update(Long id, TechnicianRequest request) {
		Technician e = findEntity(id);
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private Technician findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));
	}

	private TechnicianResponse toResponse(Technician e) {
		return new TechnicianResponse(e.getId(), e.getEmployeeId(), e.getCreatedAt());
	}
}
