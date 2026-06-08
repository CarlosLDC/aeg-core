package com.aeg.core.fiscalbookuser;

import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FiscalBookUserScopeService {

    private final EmployeeRepository employeeRepository;
    private final DistributorRepository distributorRepository;

    public Long resolveDistributorId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findById(employeeId)
                .map(employee -> employee.getBranchId())
                .flatMap(distributorRepository::findByBranch_Id)
                .map(distributor -> distributor.getId())
                .orElse(null);
    }
}
