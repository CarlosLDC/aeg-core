package com.aeg.core.employee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	boolean existsByNationalIdIgnoreCase(String nationalId);
}
