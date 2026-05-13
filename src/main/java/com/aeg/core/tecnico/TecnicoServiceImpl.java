package com.aeg.core.tecnico;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.tecnico.dto.TecnicoRequest;
import com.aeg.core.tecnico.dto.TecnicoResponse;

@Service
@Transactional
public class TecnicoServiceImpl implements TecnicoService {

	private final TecnicoRepository repository;
	private final EmployeeRepository employeeRepository;

	public TecnicoServiceImpl(TecnicoRepository repository, EmployeeRepository employeeRepository) {
		this.repository = repository;
		this.employeeRepository = employeeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TecnicoResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public TecnicoResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public TecnicoResponse create(TecnicoRequest request) {
		Tecnico e = new Tecnico();
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		return toResponse(repository.save(e));
	}

	@Override
	public TecnicoResponse update(Long id, TecnicoRequest request) {
		Tecnico e = findEntity(id);
		e.setEmployee(employeeRepository.getReferenceById(request.employeeId()));
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private Tecnico findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Tecnico not found with id: " + id));
	}

	private TecnicoResponse toResponse(Tecnico e) {
		return new TecnicoResponse(e.getId(), e.getEmployeeId(), e.getCreatedAt());
	}
}
