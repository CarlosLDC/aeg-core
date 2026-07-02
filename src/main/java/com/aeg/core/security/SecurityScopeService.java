package com.aeg.core.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.servicecenter.ServiceCenterRepository;

@Service
@Transactional(readOnly = true)
public class SecurityScopeService {

	private final BranchRepository branchRepository;
	private final ClientRepository clientRepository;
	private final DistributorRepository distributorRepository;
	private final PrinterRepository printerRepository;
	private final SealRepository sealRepository;
	private final ServiceCenterRepository serviceCenterRepository;

	public SecurityScopeService(
			BranchRepository branchRepository,
			ClientRepository clientRepository,
			DistributorRepository distributorRepository,
			PrinterRepository printerRepository,
			SealRepository sealRepository,
			ServiceCenterRepository serviceCenterRepository) {
		this.branchRepository = branchRepository;
		this.clientRepository = clientRepository;
		this.distributorRepository = distributorRepository;
		this.printerRepository = printerRepository;
		this.sealRepository = sealRepository;
		this.serviceCenterRepository = serviceCenterRepository;
	}

	public User currentUser() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
			throw new AccessDeniedException("Not authenticated");
		}
		return user;
	}

	public boolean isAdmin() {
		return currentUser().getRole() == Role.ADMIN;
	}

	public boolean isGlobalReader() {
		Role role = currentUser().getRole();
		return role == Role.ADMIN || role == Role.SENIAT;
	}

	public boolean isReadOnlyAuditor() {
		return currentUser().getRole() == Role.SENIAT;
	}

	public void assertCanWriteOperationalData() {
		if (isReadOnlyAuditor()) {
			throw new AccessDeniedException("Auditor SENIAT: acceso de solo lectura");
		}
	}

	public void assertCanWriteTechnicalService() {
		assertCanWriteOperationalData();
		if (!Role.canWriteTechnicalService(currentUser().getRole())) {
			throw new AccessDeniedException("Not allowed to write technical services");
		}
	}

	public void assertCanWriteAnnualInspection() {
		assertCanWriteOperationalData();
		if (!Role.canWriteAnnualInspection(currentUser().getRole())) {
			throw new AccessDeniedException("Not allowed to write annual inspections");
		}
	}

	public BranchScope resolveBranchScope() {
		User user = currentUser();
		return switch (user.getRole()) {
			case ADMIN, SENIAT -> BranchScope.all();
			case DISTRIBUTOR -> {
				if (user.getDistributorId() == null) {
					yield BranchScope.none();
				}
				Set<Long> ids = branchRepository.findBranchesByDistributorId(user.getDistributorId()).stream()
						.map(Branch::getId)
						.collect(Collectors.toCollection(HashSet::new));
				distributorRepository.findById(user.getDistributorId())
						.map(d -> d.getBranchId())
						.ifPresent(ids::add);
				yield BranchScope.scoped(ids);
			}
			case SERVICE_CENTER, TECHNICIAN -> {
				if (user.getBranchId() == null) {
					yield BranchScope.none();
				}
				yield BranchScope.scoped(Set.of(user.getBranchId()));
			}
		};
	}

	public void assertBranchInScope(Long branchId) {
		BranchScope scope = resolveBranchScope();
		if (!scope.allowsBranch(branchId)) {
			throw new AccessDeniedException("Not allowed to access branch id: " + branchId);
		}
	}

	public Set<Long> resolveDistributorStaffBranchIds() {
		User user = currentUser();
		if (!Role.isDistributorScoped(user.getRole()) || user.getDistributorId() == null) {
			return Collections.emptySet();
		}
		return distributorRepository.findById(user.getDistributorId())
				.map(d -> Set.of(d.getBranchId()))
				.orElse(Collections.emptySet());
	}

	public boolean isDistributorStaffBranch(Long branchId) {
		return resolveDistributorStaffBranchIds().contains(branchId);
	}

	public void assertBranchReadable(Long branchId) {
		User user = currentUser();
		if (isGlobalReader()) {
			return;
		}
		if (Role.isDistributorScoped(user.getRole())) {
			if (isDistributorStaffBranch(branchId) || resolveBranchScope().allowsBranch(branchId)) {
				return;
			}
			Long userDistributorId = user.getDistributorId();
			if (userDistributorId == null) {
				throw new AccessDeniedException("Distributor-scoped user has no distributor id");
			}
			var clientOnBranch = clientRepository.findByBranch_Id(branchId);
			if (clientOnBranch.isEmpty()) {
				return;
			}
			Long linkedDistributorId = clientOnBranch.get().getDistributorId();
			if (linkedDistributorId != null && !linkedDistributorId.equals(userDistributorId)) {
				throw new AccessDeniedException("Branch already assigned to another distributor");
			}
			return;
		}
		assertBranchInScope(branchId);
	}

	public void assertCanLinkClientToBranch(Long branchId, Long distributorId) {
		User user = currentUser();
		if (user.getRole() == Role.ADMIN) {
			return;
		}
		if (Role.isDistributorScoped(user.getRole())) {
			Long userDistributorId = user.getDistributorId();
			if (userDistributorId == null) {
				throw new AccessDeniedException("Distributor-scoped user has no distributor id");
			}
			if (distributorId == null || !userDistributorId.equals(distributorId)) {
				throw new AccessDeniedException("Not allowed to create client for this distributor");
			}
			if (resolveBranchScope().allowsBranch(branchId)) {
				return;
			}
			clientRepository.findByBranch_Id(branchId).ifPresent(existing -> {
				Long existingDistributorId = existing.getDistributorId();
				if (existingDistributorId != null && !existingDistributorId.equals(distributorId)) {
					throw new AccessDeniedException("Branch already assigned to another distributor");
				}
			});
			return;
		}
		assertBranchInScope(branchId);
	}

	public void assertPrinterInScope(Printer printer) {
		User user = currentUser();
		if (isGlobalReader()) {
			return;
		}
		if (Role.isDistributorScoped(user.getRole())) {
			Long distributorId = user.getDistributorId();
			if (distributorId == null || !distributorId.equals(printer.getDistributorId())) {
				throw new AccessDeniedException("Not allowed to access this printer");
			}
			return;
		}
		if (Role.isServiceCenterStaff(user)) {
			if (printer.getStatus() != PrinterStatus.ASIGNADA || printer.getClient() == null) {
				throw new AccessDeniedException("Not allowed to access this printer");
			}
			return;
		}
		throw new AccessDeniedException("Not allowed to access this printer");
	}

	public void assertClientInScope(Client client) {
		assertBranchInScope(client.getBranchId());
		if (Role.isDistributorScoped(currentUser().getRole())) {
			Long distributorId = currentUser().getDistributorId();
			if (distributorId == null
					|| client.getDistributorId() == null
					|| !distributorId.equals(client.getDistributorId())) {
				throw new AccessDeniedException("Not allowed to access this client");
			}
		}
	}

	public void assertInspectionInspectorInScope(User inspector) {
		if (inspector == null || !Role.canBeInspectionInspector(inspector.getRole())) {
			throw new AccessDeniedException(
					"Inspector must be an admin, distributor, or service center technician");
		}
		User user = currentUser();
		if (isAdmin()) {
			return;
		}
		if (Role.canBeInspectionInspector(user.getRole())) {
			if (!user.getId().equals(inspector.getId())) {
				throw new AccessDeniedException("Field users can only register inspections as themselves");
			}
			return;
		}
		throw new AccessDeniedException("Not allowed to assign this inspector");
	}

	public void assertTechnicalServiceSignerInScope(User signer) {
		if (signer == null || !Role.canSignTechnicalService(signer.getRole())) {
			throw new AccessDeniedException("Technical service signer must be an admin or service center technician");
		}
		if (signer.getRole() == Role.ADMIN) {
			return;
		}
		if (!Role.isServiceCenterStaff(signer)) {
			throw new AccessDeniedException("Technical service signer must be a service center technician");
		}
		if (isAdmin()) {
			return;
		}
		if (Role.isServiceCenterStaff(currentUser())) {
			return;
		}
		throw new AccessDeniedException("Not allowed to assign this technician signer");
	}

	/** @deprecated use {@link #assertInspectionInspectorInScope} or {@link #assertTechnicalServiceSignerInScope} */
	@Deprecated
	public void assertFieldUserInScope(User fieldUser) {
		assertInspectionInspectorInScope(fieldUser);
	}

	public void assertServiceCenterActorOwnsCenter(Long serviceCenterId) {
		User user = currentUser();
		if (!Role.isServiceCenterStaff(user)) {
			return;
		}
		if (user.getBranchId() == null) {
			throw new AccessDeniedException("Service center user has no branch assignment");
		}
		var center = serviceCenterRepository.findById(serviceCenterId)
				.orElseThrow(() -> new AccessDeniedException("Service center not found"));
		if (!user.getBranchId().equals(center.getBranchId())) {
			throw new AccessDeniedException("Service center user can only register visits for their center");
		}
	}

	public static boolean isPrinterInBranches(Printer printer, Set<Long> branchIds) {
		if (printer.getClient() != null && branchIds.contains(printer.getClient().getBranchId())) {
			return true;
		}
		if (printer.getDistributor() != null && branchIds.contains(printer.getDistributor().getBranchId())) {
			return true;
		}
		return false;
	}

	public List<Long> scopedBranchIdsList(BranchScope scope) {
		return scope.branchIds().stream().sorted().toList();
	}

	public List<Printer> findVisiblePrinters() {
		User user = currentUser();
		if (isGlobalReader()) {
			return printerRepository.findAll();
		}
		if (Role.isDistributorScoped(user.getRole())) {
			if (user.getDistributorId() == null) {
				return List.of();
			}
			return printerRepository.findByDistributor_Id(user.getDistributorId());
		}
		if (Role.isServiceCenterStaff(user)) {
			return printerRepository.findByStatus(PrinterStatus.ASIGNADA).stream()
					.filter(p -> p.getClient() != null)
					.toList();
		}
		return List.of();
	}

	public List<Seal> findVisibleSeals() {
		if (isGlobalReader()) {
			return sealRepository.findAll();
		}
		if (Role.isServiceCenterStaff(currentUser())) {
			return List.of();
		}
		List<Long> printerIds = findVisiblePrinters().stream().map(Printer::getId).toList();
		if (printerIds.isEmpty()) {
			return List.of();
		}
		return sealRepository.findByPrinter_IdIn(printerIds);
	}

	public List<Long> visiblePrinterIds() {
		return findVisiblePrinters().stream().map(Printer::getId).toList();
	}

	public void assertSealInScope(Seal seal) {
		if (isGlobalReader()) {
			return;
		}
		if (Role.isServiceCenterStaff(currentUser())) {
			throw new AccessDeniedException("Not allowed to access this seal");
		}
		if (seal.getPrinter() == null) {
			throw new AccessDeniedException("Not allowed to access this seal");
		}
		assertPrinterInScope(seal.getPrinter());
	}
}
