package com.aeg.core.security;

import java.util.Collections;
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
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class SecurityScopeService {

	private final BranchRepository branchRepository;
	private final ClientRepository clientRepository;
	private final DistributorRepository distributorRepository;
	private final PrinterRepository printerRepository;
	private final SealRepository sealRepository;

	public SecurityScopeService(
			BranchRepository branchRepository,
			ClientRepository clientRepository,
			DistributorRepository distributorRepository,
			PrinterRepository printerRepository,
			SealRepository sealRepository) {
		this.branchRepository = branchRepository;
		this.clientRepository = clientRepository;
		this.distributorRepository = distributorRepository;
		this.printerRepository = printerRepository;
		this.sealRepository = sealRepository;
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

	/** Lectura sin restricción de alcance (administración y auditor SENIAT). */
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
						.collect(Collectors.toSet());
				yield BranchScope.scoped(ids);
			}
			case TECHNICIAN, SERVICE_CENTER -> {
				if (user.getBranchId() == null) {
					yield BranchScope.none();
				}
				Branch ownBranch = branchRepository.findById(user.getBranchId())
						.orElseThrow(() -> new ResourceNotFoundException(
								"Branch not found with id: " + user.getBranchId()));
				Set<Long> ids = branchRepository.findByCompany_Id(ownBranch.getCompanyId()).stream()
						.map(Branch::getId)
						.collect(Collectors.toSet());
				yield BranchScope.scoped(ids);
			}
		};
	}

	public void assertBranchInScope(Long branchId) {
		BranchScope scope = resolveBranchScope();
		if (!scope.allowsBranch(branchId)) {
			throw new AccessDeniedException("Not allowed to access branch id: " + branchId);
		}
	}

	/** Sucursal de la propia distribuidora (personal interno), no sucursales de clientes. */
	public Set<Long> resolveDistributorStaffBranchIds() {
		User user = currentUser();
		if (user.getRole() != Role.DISTRIBUTOR || user.getDistributorId() == null) {
			return Collections.emptySet();
		}
		return distributorRepository.findById(user.getDistributorId())
				.map(d -> Set.of(d.getBranchId()))
				.orElse(Collections.emptySet());
	}

	public boolean isDistributorStaffBranch(Long branchId) {
		return resolveDistributorStaffBranchIds().contains(branchId);
	}

	public void assertDistributorStaffBranch(Long branchId) {
		User user = currentUser();
		if (user.getRole() == Role.ADMIN) {
			return;
		}
		if (user.getRole() == Role.DISTRIBUTOR) {
			if (isDistributorStaffBranch(branchId)) {
				return;
			}
			throw new AccessDeniedException("Not allowed to manage employees on this branch");
		}
		assertBranchInScope(branchId);
	}

	/** Lectura de sucursal: clientes en alcance o sucursal propia de la distribuidora. */
	public void assertBranchReadable(Long branchId) {
		User user = currentUser();
		if (isGlobalReader()) {
			return;
		}
		if (user.getRole() == Role.DISTRIBUTOR) {
			if (isDistributorStaffBranch(branchId) || resolveBranchScope().allowsBranch(branchId)) {
				return;
			}
			Long userDistributorId = user.getDistributorId();
			if (userDistributorId == null) {
				throw new AccessDeniedException("Distributor user has no distributor id");
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
		if (user.getRole() == Role.DISTRIBUTOR) {
			Long userDistributorId = user.getDistributorId();
			if (userDistributorId == null) {
				throw new AccessDeniedException("Distributor user has no distributor id");
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
		if (user.getRole() == Role.DISTRIBUTOR) {
			Long distributorId = user.getDistributorId();
			if (distributorId == null || !distributorId.equals(printer.getDistributorId())) {
				throw new AccessDeniedException("Not allowed to access this printer");
			}
			return;
		}
		BranchScope scope = resolveBranchScope();
		if (scope.visibility() == BranchScope.Visibility.NONE) {
			throw new AccessDeniedException("Not allowed to access this printer");
		}
		if (scope.visibility() == BranchScope.Visibility.ALL) {
			return;
		}
		if (isPrinterInBranches(printer, scope.branchIds())) {
			return;
		}
		throw new AccessDeniedException("Not allowed to access this printer");
	}

	public void assertClientInScope(Client client) {
		assertBranchInScope(client.getBranchId());
		if (currentUser().getRole() == Role.DISTRIBUTOR) {
			Long distributorId = currentUser().getDistributorId();
			if (distributorId == null
					|| client.getDistributorId() == null
					|| !distributorId.equals(client.getDistributorId())) {
				throw new AccessDeniedException("Not allowed to access this client");
			}
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
		if (user.getRole() == Role.DISTRIBUTOR) {
			if (user.getDistributorId() == null) {
				return List.of();
			}
			return printerRepository.findByDistributor_Id(user.getDistributorId());
		}
		BranchScope scope = resolveBranchScope();
		if (scope.visibility() != BranchScope.Visibility.SCOPED) {
			return List.of();
		}
		return printerRepository.findByVisibleBranchIds(scope.branchIds());
	}

	public List<Seal> findVisibleSeals() {
		if (isGlobalReader()) {
			return sealRepository.findAll();
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
		if (seal.getPrinter() == null) {
			throw new AccessDeniedException("Not allowed to access this seal");
		}
		assertPrinterInScope(seal.getPrinter());
	}
}
