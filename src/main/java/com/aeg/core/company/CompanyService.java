package com.aeg.core.company;

import java.util.List;
import java.util.Optional;

import com.aeg.core.company.dto.CompanyRequest;
import com.aeg.core.company.dto.CompanyResponse;

public interface CompanyService {
    List<CompanyResponse> findAll();
    CompanyResponse findById(Long id);
    /** Empresa por RIF (ADMIN/DISTRIBUTOR) para vincular sucursal sin estar en el listado filtrado. */
    Optional<CompanyResponse> resolveByRif(String rif);
    CompanyResponse create(CompanyRequest request);
    CompanyResponse update(Long id, CompanyRequest request);
    void delete(Long id);
}
