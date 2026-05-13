package com.aeg.core.branch;

import java.util.List;

import com.aeg.core.branch.dto.BranchRequest;
import com.aeg.core.branch.dto.BranchResponse;

public interface BranchService {
    List<BranchResponse> findAll();
    BranchResponse findById(Long id);
    BranchResponse create(BranchRequest request);
    BranchResponse update(Long id, BranchRequest request);
    void delete(Long id);
}
