package com.aeg.core.modificationrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributorperson.DistributorPersonRepository;
import com.aeg.core.employee.Employee;
import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.employee.EmployeeReviewStatus;
import com.aeg.core.employee.EmployeeType;
import com.aeg.core.employee.dto.EmployeeRequest;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.technician.TechnicianRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class ModificationRequestServiceImplTest {

	private ModificationRequestRepository modificationRequestRepository;
	private EmployeeRepository employeeRepository;
	private BranchRepository branchRepository;
	private SecurityScopeService securityScope;
	private ModificationRequestServiceImpl service;

	@BeforeEach
	void setup() {
		modificationRequestRepository = mock(ModificationRequestRepository.class);
		employeeRepository = mock(EmployeeRepository.class);
		branchRepository = mock(BranchRepository.class);
		var distributorPersonRepository = mock(DistributorPersonRepository.class);
		var technicianRepository = mock(TechnicianRepository.class);
		var annualInspectionRepository = mock(AnnualInspectionRepository.class);
		securityScope = mock(SecurityScopeService.class);

		service = new ModificationRequestServiceImpl(
				modificationRequestRepository,
				employeeRepository,
				branchRepository,
				distributorPersonRepository,
				technicianRepository,
				annualInspectionRepository,
				securityScope,
				new ObjectMapper());
	}

	@Test
	void requestUpdate_whenEmployeeAlreadyPending_throwsConflict() {
		Employee employee = new Employee();
		employee.setId(10L);
		employee.setBranch(branch(5L));
		employee.setReviewStatus(EmployeeReviewStatus.PENDING_REVIEW);
		when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

		EmployeeRequest request = new EmployeeRequest(
				"V123",
				"Empleado",
				"04120000000",
				"empleado@test.com",
				EmployeeType.ADMINISTRATIVO,
				5L);

		assertThatThrownBy(() -> service.requestUpdate(10L, request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pending review");

		verify(modificationRequestRepository, never()).save(any(ModificationRequest.class));
	}

	@Test
	void approveUpdate_appliesProposedDataAndUnblocksEmployee() {
		Employee employee = new Employee();
		employee.setId(10L);
		employee.setNationalId("V123");
		employee.setName("Antes");
		employee.setPhone("04120000000");
		employee.setEmail("antes@test.com");
		employee.setType(EmployeeType.ADMINISTRATIVO);
		employee.setBranch(branch(5L));
		employee.setReviewStatus(EmployeeReviewStatus.PENDING_REVIEW);

		Branch targetBranch = branch(9L);

		User requester = new User();
		requester.setId(99L);
		requester.setName("Distribuidor");
		requester.setRole(Role.DISTRIBUTOR);

		ModificationRequest request = new ModificationRequest();
		request.setId(100L);
		request.setEmployeeId(10L);
		request.setActionType(ModificationActionType.UPDATE);
		request.setStatus(ModificationRequestStatus.PENDING);
		request.setRequestedBy(requester);
		request.setProposedData(new ObjectMapper().valueToTree(new EmployeeRequest(
				"V555",
				"Después",
				"04129999999",
				"despues@test.com",
				EmployeeType.TECNICO,
				9L)));

		when(modificationRequestRepository.findById(100L)).thenReturn(Optional.of(request));
		when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
		when(branchRepository.findById(9L)).thenReturn(Optional.of(targetBranch));
		when(employeeRepository.existsByNationalIdIgnoreCase("V555")).thenReturn(false);
		when(modificationRequestRepository.save(any(ModificationRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.approve(100L);

		assertThat(response.status()).isEqualTo(ModificationRequestStatus.APPROVED);
		assertThat(response.currentEmployeeSnapshot()).isNotNull();
		assertThat(response.currentEmployeeSnapshot().reviewStatus()).isEqualTo(EmployeeReviewStatus.ACTIVE);
		assertThat(employee.getNationalId()).isEqualTo("V555");
		assertThat(employee.getName()).isEqualTo("Después");
		assertThat(employee.getBranchId()).isEqualTo(9L);
		assertThat(employee.getReviewStatus()).isEqualTo(EmployeeReviewStatus.ACTIVE);
	}

	private static Branch branch(Long id) {
		Branch branch = new Branch();
		branch.setId(id);
		return branch;
	}
}
