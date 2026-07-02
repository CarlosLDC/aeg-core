package com.aeg.core.client;

import org.springframework.stereotype.Component;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.servicecenter.ServiceCenterRepository;

@Component
public class ClientBranchLinkPolicy {

	public static final String REASSIGNMENT_REQUIRED_MESSAGE =
			"branch requires administrator reassignment";

	private final BranchRepository branchRepository;
	private final ClientRepository clientRepository;
	private final DistributorRepository distributorRepository;
	private final ServiceCenterRepository serviceCenterRepository;

	public ClientBranchLinkPolicy(
			BranchRepository branchRepository,
			ClientRepository clientRepository,
			DistributorRepository distributorRepository,
			ServiceCenterRepository serviceCenterRepository) {
		this.branchRepository = branchRepository;
		this.clientRepository = clientRepository;
		this.distributorRepository = distributorRepository;
		this.serviceCenterRepository = serviceCenterRepository;
	}

	public void assertFieldUserMayLinkClient(Long branchId, Long distributorId) {
		if (branchId == null) {
			return;
		}
		Branch branch = branchRepository.findById(branchId).orElse(null);
		if (branch == null) {
			return;
		}
		if (hasOperationalRole(branch)) {
			throw new IllegalArgumentException(REASSIGNMENT_REQUIRED_MESSAGE);
		}
		if (distributorRepository.findByBranch_Id(branchId).isPresent()) {
			throw new IllegalArgumentException(REASSIGNMENT_REQUIRED_MESSAGE);
		}
		if (serviceCenterRepository.findByBranch_Id(branchId).isPresent()) {
			throw new IllegalArgumentException(REASSIGNMENT_REQUIRED_MESSAGE);
		}
		clientRepository.findByBranch_Id(branchId).ifPresent(client -> {
			Long linkedDistributorId = client.getDistributorId();
			if (linkedDistributorId != null
					&& distributorId != null
					&& !linkedDistributorId.equals(distributorId)) {
				throw new IllegalArgumentException(REASSIGNMENT_REQUIRED_MESSAGE);
			}
		});
	}

	private static boolean hasOperationalRole(Branch branch) {
		BranchOrganizationRole role = branch.getOrganizationRole();
		return role == BranchOrganizationRole.DISTRIBUTOR
				|| role == BranchOrganizationRole.SERVICE_CENTER;
	}
}
