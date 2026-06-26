package com.aeg.core.branch;

import com.aeg.core.company.Company;
import com.aeg.core.company.OrganizationType;

public final class BranchOrganizationRoleSupport {

	private BranchOrganizationRoleSupport() {
	}

	public static void applyOrganizationRole(Branch branch, BranchOrganizationRole role) {
		BranchOrganizationRole resolved = role == null ? BranchOrganizationRole.NONE : role;
		branch.setOrganizationRole(resolved);
		branch.setIsDistributor(resolved == BranchOrganizationRole.DISTRIBUTOR);
		branch.setIsServiceCenter(resolved == BranchOrganizationRole.SERVICE_CENTER);
	}

	public static BranchOrganizationRole resolveFromRequest(
			BranchOrganizationRole organizationRole,
			Boolean isDistributor,
			Boolean isServiceCenter) {
		if (organizationRole != null) {
			return organizationRole;
		}
		return BranchOrganizationRole.fromLegacyFlags(isDistributor, isServiceCenter);
	}

	public static void assertOperationalRoleAllowed(Company company, BranchOrganizationRole role) {
		if (role == BranchOrganizationRole.NONE) {
			return;
		}
		if (company != null && company.getOrganizationType() == OrganizationType.FACTORY) {
			throw new IllegalArgumentException(
					"Las sucursales de la empresa fábrica no pueden ser distribuidora ni centro de servicio");
		}
	}

	public static void assertNotConflictingRole(Branch branch, BranchOrganizationRole desiredRole) {
		if (desiredRole == BranchOrganizationRole.DISTRIBUTOR
				&& branch.getOrganizationRole() == BranchOrganizationRole.SERVICE_CENTER) {
			throw new IllegalArgumentException(
					"La sucursal ya es centro de servicio; no puede ser distribuidora");
		}
		if (desiredRole == BranchOrganizationRole.SERVICE_CENTER
				&& branch.getOrganizationRole() == BranchOrganizationRole.DISTRIBUTOR) {
			throw new IllegalArgumentException(
					"La sucursal ya es distribuidora; no puede ser centro de servicio");
		}
	}
}
