package com.aeg.core.servicecenter;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.servicecenter.dto.ServiceCenterRequest;
import com.aeg.core.servicecenter.dto.ServiceCenterResponse;

@Service
@Transactional
public class ServiceCenterServiceImpl implements ServiceCenterService {

	private final ServiceCenterRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;

	public ServiceCenterServiceImpl(ServiceCenterRepository repository, com.aeg.core.branch.BranchRepository branchRepository) {
		this.repository = repository;
		this.branchRepository = branchRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ServiceCenterResponse> findAll() {
		return repository.findAll().stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ServiceCenterResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Override
	public ServiceCenterResponse create(ServiceCenterRequest request) {
		ServiceCenter serviceCenter = new ServiceCenter();
		serviceCenter.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
		return toResponse(repository.save(serviceCenter));
	}

	@Override
	public ServiceCenterResponse update(Long id, ServiceCenterRequest request) {
		ServiceCenter serviceCenter = findEntityById(id);
		serviceCenter.setBranch(branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId())));
		return toResponse(repository.save(serviceCenter));
	}

	@Override
	public void delete(Long id) {
		ServiceCenter serviceCenter = findEntityById(id);
		repository.delete(serviceCenter);
	}

	private ServiceCenter findEntityById(Long id) {
		return repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Service center not found with id: " + id));
	}

	private ServiceCenterResponse toResponse(ServiceCenter serviceCenter) {
		return new ServiceCenterResponse(
			serviceCenter.getId(),
			serviceCenter.getBranchId(),
			serviceCenter.getCreatedAt()
		);
	}
}