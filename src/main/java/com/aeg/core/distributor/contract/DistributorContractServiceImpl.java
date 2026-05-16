package com.aeg.core.distributor.contract;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.distributor.contract.dto.DistributorContractRequest;
import com.aeg.core.distributor.contract.dto.DistributorContractResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class DistributorContractServiceImpl implements DistributorContractService {

	private final DistributorContractRepository repository;
	private final DistributorRepository distributorRepository;

	public DistributorContractServiceImpl(
			DistributorContractRepository repository,
			DistributorRepository distributorRepository) {
		this.repository = repository;
		this.distributorRepository = distributorRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DistributorContractResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DistributorContractResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public DistributorContractResponse create(DistributorContractRequest request) {
		DistributorContract e = new DistributorContract();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public DistributorContractResponse update(Long id, DistributorContractRequest request) {
		DistributorContract e = findEntity(id);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private DistributorContract findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Distributor contract not found with id: " + id));
	}

	private void applyRequest(DistributorContract e, DistributorContractRequest request) {
		e.setDistributor(distributorRepository.findById(request.distributorId())
				.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.distributorId())));
		e.setStartDate(request.startDate());
		e.setEndDate(request.endDate());
		e.setPhotoUrls(request.photoUrls().toArray(String[]::new));
	}

	private DistributorContractResponse toResponse(DistributorContract e) {
		return new DistributorContractResponse(
				e.getId(),
				e.getDistributorId(),
				e.getStartDate(),
				e.getEndDate(),
				e.getCreatedAt(),
				e.getPhotoUrls() == null ? List.of() : Arrays.asList(e.getPhotoUrls()));
	}
}
