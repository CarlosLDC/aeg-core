package com.aeg.core.employee;

import java.util.List;

import com.aeg.core.employee.dto.EmployeeRequest;
import com.aeg.core.employee.dto.EmployeeResponse;

public interface EmployeeService {

	List<EmployeeResponse> findAll();

	EmployeeResponse findById(Long id);

	EmployeeResponse create(EmployeeRequest request);

	EmployeeResponse update(Long id, EmployeeRequest request);

	void delete(Long id);
}
