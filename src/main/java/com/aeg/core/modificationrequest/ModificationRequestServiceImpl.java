package com.aeg.core.modificationrequest;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributorperson.DistributorPersonRepository;
import com.aeg.core.employee.Employee;
import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.employee.EmployeeReviewStatus;
import com.aeg.core.employee.dto.EmployeeRequest;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.modificationrequest.dto.EmployeeSnapshotResponse;
import com.aeg.core.modificationrequest.dto.ModificationRequestDetailResponse;
import com.aeg.core.modificationrequest.dto.ModificationRequestListItemResponse;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.technician.TechnicianRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class ModificationRequestServiceImpl implements ModificationRequestService {

	private final ModificationRequestRepository repository;
	private final EmployeeRepository employeeRepository;
	private final BranchRepository branchRepository;
	private final DistributorPersonRepository distributorPersonRepository;
	private final TechnicianRepository technicianRepository;
	private final AnnualInspectionRepository annualInspectionRepository;
	private final SecurityScopeService securityScope;
	private final ObjectMapper objectMapper;

	public ModificationRequestServiceImpl(
			ModificationRequestRepository repository,
			EmployeeRepository employeeRepository,
			BranchRepository branchRepository,
			DistributorPersonRepository distributorPersonRepository,
			TechnicianRepository technicianRepository,
			AnnualInspectionRepository annualInspectionRepository,
			SecurityScopeService securityScope,
			ObjectMapper objectMapper) {
		this.repository = repository;
		this.employeeRepository = employeeRepository;
		this.branchRepository = branchRepository;
		this.distributorPersonRepository = distributorPersonRepository;
		this.technicianRepository = technicianRepository;
		this.annualInspectionRepository = annualInspectionRepository;
		this.securityScope = securityScope;
		this.objectMapper = objectMapper;
	}

	@Override
	public ModificationRequestDetailResponse requestUpdate(Long employeeId, EmployeeRequest proposedData) {
		Employee employee = findEmployee(employeeId);
		securityScope.assertDistributorStaffBranch(employee.getBranchId());
		assertEmployeeNotPending(employee);

		var branch = branchRepository.findById(proposedData.branchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + proposedData.branchId()));
		securityScope.assertDistributorStaffBranch(branch.getId());

		if (!employee.getNationalId().equalsIgnoreCase(proposedData.nationalId())
				&& employeeRepository.existsByNationalIdIgnoreCase(proposedData.nationalId())) {
			throw new IllegalArgumentException("nationalId already exists: " + proposedData.nationalId());
		}

		User requestedBy = securityScope.currentUser();
		employee.setReviewStatus(EmployeeReviewStatus.PENDING_REVIEW);
		employeeRepository.save(employee);

		ModificationRequest request = new ModificationRequest();
		request.setEmployeeId(employee.getId());
		request.setActionType(ModificationActionType.UPDATE);
		request.setProposedData(objectMapper.valueToTree(proposedData));
		request.setRequestedBy(requestedBy);
		request.setStatus(ModificationRequestStatus.PENDING);

		return toDetailResponse(repository.save(request), employee);
	}

	@Override
	public ModificationRequestDetailResponse requestDelete(Long employeeId) {
		Employee employee = findEmployee(employeeId);
		securityScope.assertDistributorStaffBranch(employee.getBranchId());
		assertEmployeeNotPending(employee);

		User requestedBy = securityScope.currentUser();
		employee.setReviewStatus(EmployeeReviewStatus.PENDING_REVIEW);
		employeeRepository.save(employee);

		ModificationRequest request = new ModificationRequest();
		request.setEmployeeId(employee.getId());
		request.setActionType(ModificationActionType.DELETE);
		request.setRequestedBy(requestedBy);
		request.setStatus(ModificationRequestStatus.PENDING);

		return toDetailResponse(repository.save(request), employee);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModificationRequestListItemResponse> findByStatus(ModificationRequestStatus status) {
		ModificationRequestStatus target = status == null ? ModificationRequestStatus.PENDING : status;
		return repository.findByStatusOrderByCreatedAtDesc(target).stream()
				.map(this::toListItemResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ModificationRequestDetailResponse findById(Long id) {
		ModificationRequest request = findRequest(id);
		Employee employee = employeeRepository.findById(request.getEmployeeId()).orElse(null);
		return toDetailResponse(request, employee);
	}

	@Override
	public ModificationRequestDetailResponse approve(Long id) {
		ModificationRequest request = findPendingRequest(id);
		Employee employee = findEmployee(request.getEmployeeId());
		assertEmployeePending(employee);

		if (request.getActionType() == ModificationActionType.UPDATE) {
			EmployeeRequest proposed = toEmployeeRequest(request.getProposedData());
			var branch = branchRepository.findById(proposed.branchId())
					.orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + proposed.branchId()));

			if (!employee.getNationalId().equalsIgnoreCase(proposed.nationalId())
					&& employeeRepository.existsByNationalIdIgnoreCase(proposed.nationalId())) {
				throw new IllegalArgumentException("nationalId already exists: " + proposed.nationalId());
			}

			employee.setNationalId(proposed.nationalId());
			employee.setName(proposed.name());
			employee.setPhone(proposed.phone());
			employee.setEmail(proposed.email());
			employee.setType(proposed.type());
			employee.setBranch(branch);
			employee.setReviewStatus(EmployeeReviewStatus.ACTIVE);
			employeeRepository.save(employee);
		} else {
			if (annualInspectionRepository.existsByEmployee_Id(employee.getId())) {
				throw new IllegalArgumentException(
						"employee has annual inspections and cannot be deleted: " + employee.getId());
			}
			distributorPersonRepository.findByEmployee_Id(employee.getId()).ifPresent(distributorPersonRepository::delete);
			technicianRepository.findByEmployee_Id(employee.getId()).ifPresent(technicianRepository::delete);
			employeeRepository.delete(employee);
		}

		request.setStatus(ModificationRequestStatus.APPROVED);
		ModificationRequest saved = repository.save(request);
		Employee current = employeeRepository.findById(request.getEmployeeId()).orElse(null);
		return toDetailResponse(saved, current);
	}

	@Override
	public ModificationRequestDetailResponse reject(Long id) {
		ModificationRequest request = findPendingRequest(id);
		Employee employee = findEmployee(request.getEmployeeId());
		assertEmployeePending(employee);

		employee.setReviewStatus(EmployeeReviewStatus.ACTIVE);
		employeeRepository.save(employee);

		request.setStatus(ModificationRequestStatus.REJECTED);
		return toDetailResponse(repository.save(request), employee);
	}

	private ModificationRequest findRequest(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Modification request not found with id: " + id));
	}

	private ModificationRequest findPendingRequest(Long id) {
		ModificationRequest request = findRequest(id);
		if (request.getStatus() != ModificationRequestStatus.PENDING) {
			throw new IllegalArgumentException("modification request is no longer pending");
		}
		return request;
	}

	private Employee findEmployee(Long id) {
		return employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
	}

	private void assertEmployeeNotPending(Employee employee) {
		if (employee.getReviewStatus() == EmployeeReviewStatus.PENDING_REVIEW) {
			throw new IllegalArgumentException("employee has a pending review request");
		}
	}

	private void assertEmployeePending(Employee employee) {
		if (employee.getReviewStatus() != EmployeeReviewStatus.PENDING_REVIEW) {
			throw new IllegalArgumentException("employee is not in pending review state");
		}
	}

	private EmployeeRequest toEmployeeRequest(JsonNode proposedData) {
		if (proposedData == null || proposedData.isNull()) {
			throw new IllegalArgumentException("proposedData is required for UPDATE requests");
		}
		return objectMapper.convertValue(proposedData, EmployeeRequest.class);
	}

	private ModificationRequestListItemResponse toListItemResponse(ModificationRequest request) {
		Employee employee = employeeRepository.findById(request.getEmployeeId()).orElse(null);
		return new ModificationRequestListItemResponse(
				request.getId(),
				request.getEmployeeId(),
				employee != null ? employee.getName() : "Empleado eliminado",
				request.getActionType(),
				request.getStatus(),
				request.getRequestedBy().getId(),
				request.getRequestedBy().getName(),
				request.getCreatedAt());
	}

	private ModificationRequestDetailResponse toDetailResponse(ModificationRequest request, Employee employee) {
		return new ModificationRequestDetailResponse(
				request.getId(),
				request.getEmployeeId(),
				request.getActionType(),
				request.getStatus(),
				request.getProposedData(),
				toSnapshot(employee),
				request.getRequestedBy().getId(),
				request.getRequestedBy().getName(),
				request.getCreatedAt());
	}

	private EmployeeSnapshotResponse toSnapshot(Employee employee) {
		if (employee == null) {
			return null;
		}
		return new EmployeeSnapshotResponse(
				employee.getId(),
				employee.getNationalId(),
				employee.getName(),
				employee.getPhone(),
				employee.getEmail(),
				employee.getType(),
				employee.getBranchId(),
				employee.getReviewStatus());
	}
}
