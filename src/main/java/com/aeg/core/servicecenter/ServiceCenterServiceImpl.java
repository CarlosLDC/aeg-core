package com.aeg.core.servicecenter;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.security.BranchScope;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.servicecenter.dto.ServiceCenterRequest;
import com.aeg.core.servicecenter.dto.ServiceCenterResponse;

@Service
@Transactional
public class ServiceCenterServiceImpl implements ServiceCenterService {

	private final ServiceCenterRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;
	private final SecurityScopeService securityScope;

	public ServiceCenterServiceImpl(
			ServiceCenterRepository repository,
			com.aeg.core.branch.BranchRepository branchRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.branchRepository = branchRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ServiceCenterResponse> findAll() {
		if (securityScope.isAdmin()) {
			return repository.findAll().stream().map(this::toResponse).toList();
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
	public ServiceCenterResponse findById(Long id) {
		ServiceCenter serviceCenter = findEntityById(id);
		securityScope.assertBranchInScope(serviceCenter.getBranchId());
		return toResponse(serviceCenter);
	}

	@Override
	public ServiceCenterResponse create(ServiceCenterRequest request) {
		ServiceCenter serviceCenter = new ServiceCenter();
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertBranchInScope(branch.getId());
		serviceCenter.setBranch(branch);
		return toResponse(repository.save(serviceCenter));
	}

	@Override
	public ServiceCenterResponse update(Long id, ServiceCenterRequest request) {
		ServiceCenter serviceCenter = findEntityById(id);
		securityScope.assertBranchInScope(serviceCenter.getBranchId());
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertBranchInScope(branch.getId());
		serviceCenter.setBranch(branch);
		return toResponse(repository.save(serviceCenter));
	}

	@Override
	public void delete(Long id) {
		ServiceCenter serviceCenter = findEntityById(id);
		securityScope.assertBranchInScope(serviceCenter.getBranchId());
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
				serviceCenter.getCreatedAt());
	}
}
