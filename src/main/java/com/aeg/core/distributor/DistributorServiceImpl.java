package com.aeg.core.distributor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributor.dto.DistributorRequest;
import com.aeg.core.distributor.dto.DistributorResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class DistributorServiceImpl implements DistributorService {

	private final DistributorRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;

	public DistributorServiceImpl(DistributorRepository repository, com.aeg.core.branch.BranchRepository branchRepository) {
		this.repository = repository;
		this.branchRepository = branchRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DistributorResponse> findAll() {
		return repository.findAll().stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DistributorResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Override
	public DistributorResponse create(DistributorRequest request) {
		Distributor distributor = new Distributor();
		distributor.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
		return toResponse(repository.save(distributor));
	}

	@Override
	public DistributorResponse update(Long id, DistributorRequest request) {
		Distributor distributor = findEntityById(id);
		distributor.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
		return toResponse(repository.save(distributor));
	}

	@Override
	public void delete(Long id) {
		Distributor distributor = findEntityById(id);
		repository.delete(distributor);
	}

	private Distributor findEntityById(Long id) {
		return repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));
	}

	private DistributorResponse toResponse(Distributor distributor) {
		return new DistributorResponse(distributor.getId(), distributor.getBranchId(), distributor.getCreatedAt());
	}
}