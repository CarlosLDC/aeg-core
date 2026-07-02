package com.aeg.core.security;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.security.UserController.UserRegistrationRequest;
import com.aeg.core.security.UserController.UserUpdateRequest;
import com.aeg.core.servicecenter.ServiceCenterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleAssignmentService {

	private final DistributorRepository distributorRepository;
	private final BranchRepository branchRepository;
	private final ServiceCenterRepository serviceCenterRepository;

	public record RoleResolution(Role role, HttpStatus errorStatus) {
		public static RoleResolution ok(Role role) {
			return new RoleResolution(role, null);
		}

		public static RoleResolution error(HttpStatus status) {
			return new RoleResolution(null, status);
		}

		public boolean hasError() {
			return errorStatus != null;
		}
	}

	public RoleResolution resolveForCreate(UserRegistrationRequest request) {
		Role requestedRole = parseRole(request.getRole());
		if (requestedRole == null) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}
		return resolveOperational(requestedRole, request.getDistributorId(), request.getBranchId());
	}

	public RoleResolution resolveForUpdate(UserUpdateRequest request, User existing) {
		Role requestedRole = request.getRole() != null && !request.getRole().isBlank()
				? parseRole(request.getRole())
				: existing.getRole();
		if (requestedRole == null) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}
		if (requestedRole == Role.ADMIN || requestedRole == Role.SENIAT) {
			if (request.getDistributorId() != null || request.getBranchId() != null) {
				return RoleResolution.error(HttpStatus.BAD_REQUEST);
			}
			return RoleResolution.ok(requestedRole);
		}
		Long distributorId = request.getDistributorId() != null
				? request.getDistributorId()
				: existing.getDistributorId();
		Long branchId = request.getBranchId() != null
				? request.getBranchId()
				: existing.getBranchId();
		return resolveOperational(requestedRole, distributorId, branchId);
	}

	private RoleResolution resolveOperational(Role requestedRole, Long distributorId, Long branchId) {
		if (requestedRole == Role.ADMIN || requestedRole == Role.SENIAT) {
			if (distributorId != null || branchId != null) {
				return RoleResolution.error(HttpStatus.BAD_REQUEST);
			}
			return RoleResolution.ok(requestedRole);
		}

		if (requestedRole == Role.SERVICE_CENTER) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}

		if (distributorId != null && branchId != null) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}

		if (distributorId != null) {
			RoleResolution validation = validateDistributorAssignment(distributorId);
			if (validation.hasError()) {
				return validation;
			}
			return RoleResolution.ok(Role.DISTRIBUTOR);
		}

		if (branchId != null) {
			RoleResolution validation = validateServiceCenterAssignment(branchId);
			if (validation.hasError()) {
				return validation;
			}
			return RoleResolution.ok(Role.TECHNICIAN);
		}

		if (requestedRole == Role.DISTRIBUTOR || requestedRole == Role.TECHNICIAN) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}

		return RoleResolution.error(HttpStatus.BAD_REQUEST);
	}

	private RoleResolution validateDistributorAssignment(Long distributorId) {
		var distributor = distributorRepository.findById(distributorId);
		if (distributor.isEmpty()) {
			return RoleResolution.error(HttpStatus.NOT_FOUND);
		}
		Branch branch = distributor.get().getBranch();
		if (branch == null) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}
		BranchOrganizationRole role = branch.getOrganizationRole();
		if (role == null || role == BranchOrganizationRole.NONE) {
			role = BranchOrganizationRole.fromLegacyFlags(
					branch.getIsDistributor(), branch.getIsServiceCenter());
		}
		if (role != BranchOrganizationRole.DISTRIBUTOR) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}
		return RoleResolution.ok(Role.DISTRIBUTOR);
	}

	private RoleResolution validateServiceCenterAssignment(Long branchId) {
		Branch branch = branchRepository.findById(branchId).orElse(null);
		if (branch == null || branch.getOrganizationRole() != BranchOrganizationRole.SERVICE_CENTER) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}
		if (serviceCenterRepository.findByBranch_Id(branchId).isEmpty()) {
			return RoleResolution.error(HttpStatus.BAD_REQUEST);
		}
		return RoleResolution.ok(Role.TECHNICIAN);
	}

	private Role parseRole(String rawRole) {
		if (rawRole == null || rawRole.isBlank()) {
			return null;
		}
		try {
			return Role.valueOf(rawRole.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
