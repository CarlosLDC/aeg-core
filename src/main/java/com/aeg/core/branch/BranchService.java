package com.aeg.core.branch;

import java.util.List;
import java.util.Optional;

import com.aeg.core.branch.dto.BranchRequest;
import com.aeg.core.branch.dto.BranchResponse;

public interface BranchService {
    List<BranchResponse> findAll();
    BranchResponse findById(Long id);
    /** Sucursal existente por empresa y ubicación (alta de cliente / reintentos). */
    Optional<BranchResponse> lookupByCompanyLocation(Long companyId, String city, String state);
    BranchResponse create(BranchRequest request);
    BranchResponse update(Long id, BranchRequest request);
    void delete(Long id);
}
