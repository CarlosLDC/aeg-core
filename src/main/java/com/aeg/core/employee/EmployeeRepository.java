package com.aeg.core.employee;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	boolean existsByNationalIdIgnoreCase(String nationalId);

	List<Employee> findByBranch_IdIn(Collection<Long> branchIds);
}
