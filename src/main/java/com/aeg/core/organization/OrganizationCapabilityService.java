package com.aeg.core.organization;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.OrganizationType;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;

@Service
public class OrganizationCapabilityService {

	private final SecurityScopeService securityScope;
	private final BranchRepository branchRepository;
	private final DistributorRepository distributorRepository;

	public OrganizationCapabilityService(
			SecurityScopeService securityScope,
			BranchRepository branchRepository,
			DistributorRepository distributorRepository) {
		this.securityScope = securityScope;
		this.branchRepository = branchRepository;
		this.distributorRepository = distributorRepository;
	}

	public OrganizationProfile resolve(Company company, Branch branch) {
		if (company != null && company.getOrganizationType() == OrganizationType.FACTORY) {
			return OrganizationProfile.FACTORY;
		}
		if (branch == null) {
			return OrganizationProfile.NONE;
		}
		BranchOrganizationRole role = branch.getOrganizationRole();
		if (role == null || role == BranchOrganizationRole.NONE) {
			role = BranchOrganizationRole.fromLegacyFlags(
					branch.getIsDistributor(), branch.getIsServiceCenter());
		}
		return switch (role) {
			case SERVICE_CENTER -> OrganizationProfile.SERVICE_CENTER;
			case DISTRIBUTOR -> OrganizationProfile.DISTRIBUTOR;
			case NONE -> OrganizationProfile.NONE;
		};
	}

	public boolean hasCapability(OrganizationProfile profile, OrgCapability capability) {
		return switch (capability) {
			case ASSIGN_TO_DISTRIBUTOR -> profile == OrganizationProfile.FACTORY;
			case ENAJENAR -> profile == OrganizationProfile.FACTORY
					|| profile == OrganizationProfile.SERVICE_CENTER
					|| profile == OrganizationProfile.DISTRIBUTOR;
			case WRITE_TECHNICAL_SERVICE -> profile == OrganizationProfile.FACTORY
					|| profile == OrganizationProfile.SERVICE_CENTER;
			case WRITE_ANNUAL_INSPECTION -> profile == OrganizationProfile.FACTORY
					|| profile == OrganizationProfile.SERVICE_CENTER
					|| profile == OrganizationProfile.DISTRIBUTOR;
		};
	}

	public void assertCapability(OrganizationProfile profile, OrgCapability capability) {
		if (!hasCapability(profile, capability)) {
			throw new AccessDeniedException("Organization profile cannot perform: " + capability);
		}
	}

	/**
	 * Perfil operativo del usuario autenticado según su rol y asignación organizacional.
	 */
	public OrganizationProfile resolveActorProfile() {
		User user = securityScope.currentUser();
		if (user.getRole() == Role.ADMIN) {
			return OrganizationProfile.FACTORY;
		}
		if (Role.isServiceCenterStaff(user)) {
			return branchRepository.findById(user.getBranchId())
					.map(branch -> resolve(branch.getCompany(), branch))
					.orElse(OrganizationProfile.NONE);
		}
		if (Role.isDistributorScoped(user.getRole()) && user.getDistributorId() != null) {
			return distributorRepository.findById(user.getDistributorId())
					.map(distributor -> {
						Branch branch = distributor.getBranch();
						OrganizationProfile profile = resolve(
								branch != null ? branch.getCompany() : null, branch);
						if (profile == OrganizationProfile.NONE) {
							return OrganizationProfile.DISTRIBUTOR;
						}
						return profile;
					})
					.orElse(OrganizationProfile.DISTRIBUTOR);
		}
		return OrganizationProfile.NONE;
	}

	public void assertActorCan(OrgCapability capability) {
		assertCapability(resolveActorProfile(), capability);
	}
}
