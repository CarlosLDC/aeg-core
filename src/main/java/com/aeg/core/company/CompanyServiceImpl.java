package com.aeg.core.company;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.dto.CompanyRequest;
import com.aeg.core.company.dto.CompanyResponse;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import org.springframework.security.access.AccessDeniedException;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository repository;
    private final BranchRepository branchRepository;
    private final ClientRepository clientRepository;
    private final SecurityScopeService securityScope;

    public CompanyServiceImpl(
            CompanyRepository repository,
            BranchRepository branchRepository,
            ClientRepository clientRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.clientRepository = clientRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {
        User currentUser = securityScope.currentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return repository.findAll().stream().map(this::toResponse).toList();
        }
        if (Role.isDistributorScoped(currentUser.getRole()) && currentUser.getDistributorId() != null) {
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
        Company company = findEntityById(id);
        assertCompanyReadable(company.getId());
        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyResponse> resolveByRif(String rif) {
        User currentUser = securityScope.currentUser();
        if (currentUser.getRole() != Role.ADMIN && !Role.isDistributorScoped(currentUser.getRole())) {
            return Optional.empty();
        }
        String normalized = normalizeRif(rif);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return repository
                .findByRif(normalized)
                .or(() -> repository.findByNormalizedRif(normalized))
                .map(this::toResponse);
    }

    private static String normalizeRif(String rif) {
        if (rif == null) {
            return "";
        }
        return rif.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    @Override
    public CompanyResponse create(CompanyRequest request) {
        if (request.contributorType() == null) {
            throw new IllegalArgumentException("El tipo de contribuyente es obligatorio.");
        }
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
        assertCompanyReadable(c.getId());
        if (Role.isDistributorScoped(securityScope.currentUser().getRole())
                && clientRepository.existsByBranch_Company_Id(c.getId())) {
            throw new IllegalArgumentException("client updates must be requested for review");
        }
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
        assertCompanyReadable(c.getId());
        repository.delete(c);
    }

    private void assertCompanyReadable(Long companyId) {
        User currentUser = securityScope.currentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (Role.isDistributorScoped(currentUser.getRole()) && currentUser.getDistributorId() != null) {
            boolean inScope = repository.findCompaniesByDistributorId(currentUser.getDistributorId())
                    .stream()
                    .anyMatch(c -> c.getId().equals(companyId));
            if (inScope) {
                return;
            }
        }
        throw new AccessDeniedException("Not allowed to access company id: " + companyId);
    }

    private Company findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    private CompanyResponse toResponse(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getBusinessName(),
                c.getCreatedAt(),
                c.getRif(),
                c.getContributorType(),
                c.getOrganizationType());
    }
}
