package com.aeg.core.company;

import java.util.List;

import com.aeg.core.company.dto.CompanyRequest;
import com.aeg.core.company.dto.CompanyResponse;

public interface CompanyService {
    List<CompanyResponse> findAll();
    CompanyResponse findById(Long id);
    CompanyResponse create(CompanyRequest request);
    CompanyResponse update(Long id, CompanyRequest request);
    void delete(Long id);
}
