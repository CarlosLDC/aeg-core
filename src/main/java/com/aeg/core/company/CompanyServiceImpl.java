package com.aeg.core.company;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.company.dto.CompanyRequest;
import com.aeg.core.company.dto.CompanyResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository repository;

    public CompanyServiceImpl(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Override
    public CompanyResponse create(CompanyRequest request) {
        if (repository.existsByRif(request.rif())) {
            throw new IllegalArgumentException("rif already exists: " + request.rif());
        }
        Company c = new Company();
        c.setBusinessName(request.businessName());
        c.setRif(request.rif());
        c.setContributorType(request.contributorType());
        return toResponse(repository.save(c));
    }

    @Override
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company c = findEntityById(id);
        if (!c.getRif().equals(request.rif()) && repository.existsByRif(request.rif())) {
            throw new IllegalArgumentException("rif already exists: " + request.rif());
        }
        c.setBusinessName(request.businessName());
        c.setRif(request.rif());
        c.setContributorType(request.contributorType());
        return toResponse(repository.save(c));
    }

    @Override
    public void delete(Long id) {
        Company c = findEntityById(id);
        repository.delete(c);
    }

    private Company findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    private CompanyResponse toResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getBusinessName(), c.getCreatedAt(), c.getRif(), c.getContributorType());
    }
}
