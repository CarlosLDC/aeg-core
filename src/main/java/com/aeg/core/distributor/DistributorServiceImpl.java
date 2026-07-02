package com.aeg.core.distributor;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchOrganizationRoleSupport;
import com.aeg.core.distributor.dto.DistributorRequest;
import com.aeg.core.distributor.dto.DistributorResponse;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class DistributorServiceImpl implements DistributorService {

	private final DistributorRepository repository;
	private final com.aeg.core.branch.BranchRepository branchRepository;
	private final SecurityScopeService securityScope;

	public DistributorServiceImpl(
			DistributorRepository repository,
			com.aeg.core.branch.BranchRepository branchRepository,
			SecurityScopeService securityScope) {
		this.repository = repository;
		this.branchRepository = branchRepository;
		this.securityScope = securityScope;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DistributorResponse> findAll() {
		if (securityScope.isAdmin()) {
			return repository.findAll().stream().map(this::toResponse).toList();
		}
		User user = securityScope.currentUser();
		if (Role.isDistributorScoped(user.getRole())) {
			if (user.getDistributorId() == null) {
				return List.of();
			}
			return repository.findById(user.getDistributorId())
					.stream()
					.map(this::toResponse)
					.toList();
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
	public DistributorResponse findById(Long id) {
		Distributor distributor = findEntityById(id);
		User user = securityScope.currentUser();
		if (Role.isDistributorScoped(user.getRole())) {
			if (user.getDistributorId() == null || !user.getDistributorId().equals(id)) {
				throw new AccessDeniedException("Not allowed to access distributor id: " + id);
			}
			return toResponse(distributor);
		}
		securityScope.assertBranchInScope(distributor.getBranchId());
		return toResponse(distributor);
	}

	@Override
	public DistributorResponse create(DistributorRequest request) {
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertBranchInScope(branch.getId());
		validateDistributorBranch(branch);
		BranchOrganizationRoleSupport.applyOrganizationRole(branch, BranchOrganizationRole.DISTRIBUTOR);
		branchRepository.save(branch);
		Distributor distributor = new Distributor();
		distributor.setBranch(branch);
		return toResponse(repository.save(distributor));
	}

	@Override
	public DistributorResponse update(Long id, DistributorRequest request) {
		Distributor distributor = findEntityById(id);
		securityScope.assertBranchInScope(distributor.getBranchId());
		var branch = branchRepository.findById(request.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));
		securityScope.assertBranchInScope(branch.getId());
		validateDistributorBranch(branch);
		BranchOrganizationRoleSupport.applyOrganizationRole(branch, BranchOrganizationRole.DISTRIBUTOR);
		branchRepository.save(branch);
		distributor.setBranch(branch);
		return toResponse(repository.save(distributor));
	}

	@Override
	public void delete(Long id) {
		Distributor distributor = findEntityById(id);
		securityScope.assertBranchInScope(distributor.getBranchId());
		Branch branch = distributor.getBranch();
		if (branch != null && branch.getOrganizationRole() == BranchOrganizationRole.DISTRIBUTOR) {
			BranchOrganizationRoleSupport.applyOrganizationRole(branch, BranchOrganizationRole.NONE);
			branchRepository.save(branch);
		}
		repository.delete(distributor);
	}

	private void validateDistributorBranch(Branch branch) {
		BranchOrganizationRoleSupport.assertOperationalRoleAllowed(branch.getCompany(), BranchOrganizationRole.DISTRIBUTOR);
		BranchOrganizationRoleSupport.assertNotConflictingRole(branch, BranchOrganizationRole.DISTRIBUTOR);
		if (repository.findByBranch_Id(branch.getId()).isPresent()) {
			throw new IllegalArgumentException("Branch already has a distributor record");
		}
	}

	private Distributor findEntityById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));
	}

	private DistributorResponse toResponse(Distributor distributor) {
		return new DistributorResponse(distributor.getId(), distributor.getBranchId(), distributor.getCreatedAt());
	}
}
