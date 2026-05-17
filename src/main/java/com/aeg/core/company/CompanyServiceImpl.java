package com.aeg.core.company;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.company.dto.CompanyRequest;
import com.aeg.core.company.dto.CompanyResponse;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository repository;
    private final BranchRepository branchRepository;
    private final SecurityScopeService securityScope;

    public CompanyServiceImpl(
            CompanyRepository repository,
            BranchRepository branchRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {
        User currentUser = securityScope.currentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return repository.findAll().stream().map(this::toResponse).toList();
        }
        if (currentUser.getRole() == Role.DISTRIBUTOR && currentUser.getDistributorId() != null) {
            return repository.findCompaniesByDistributorId(currentUser.getDistributorId()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        BranchScope scope = securityScope.resolveBranchScope();
        if (scope.visibility() != BranchScope.Visibility.SCOPED) {
            return List.of();
        }
        var companyIds = branchRepository.findByIdIn(scope.branchIds()).stream()
                .map(Branch::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(companyIds).stream().map(this::toResponse).toList();
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
