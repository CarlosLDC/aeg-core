package com.aeg.core.branch;

public enum BranchOrganizationRole {
	NONE,
	DISTRIBUTOR,
	SERVICE_CENTER;

	public static BranchOrganizationRole fromLegacyFlags(Boolean isDistributor, Boolean isServiceCenter) {
		boolean distributor = Boolean.TRUE.equals(isDistributor);
		boolean serviceCenter = Boolean.TRUE.equals(isServiceCenter);
		if (distributor && serviceCenter) {
			return SERVICE_CENTER;
		}
		if (distributor) {
			return DISTRIBUTOR;
		}
		if (serviceCenter) {
			return SERVICE_CENTER;
		}
		return NONE;
	}
}
