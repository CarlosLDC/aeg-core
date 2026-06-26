package com.aeg.core.branch;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.dto.BranchRequest;
import com.aeg.core.branch.dto.BranchResponse;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.security.BranchScope;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.UserRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository repository;
    private final com.aeg.core.company.CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final SecurityScopeService securityScope;

    public BranchServiceImpl(
            BranchRepository repository,
            com.aeg.core.company.CompanyRepository companyRepository,
            ClientRepository clientRepository,
            UserRepository userRepository,
            SecurityScopeService securityScope) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.securityScope = securityScope;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        BranchScope scope = securityScope.resolveBranchScope();
        return switch (scope.visibility()) {
            case ALL -> repository.findAll().stream().map(this::toResponse).toList();
            case NONE -> List.of();
            case SCOPED -> repository.findByIdIn(scope.branchIds()).stream().map(this::toResponse).toList();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse findById(Long id) {
        Branch branch = findEntityById(id);
        securityScope.assertBranchReadable(branch.getId());
        return toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchResponse> lookupByCompanyLocation(Long companyId, String city, String state) {
        if (companyId == null || city == null || city.isBlank() || state == null || state.isBlank()) {
            return Optional.empty();
        }
        return repository
                .findFirstByCompany_IdAndCityIgnoreCaseAndStateIgnoreCase(
                        companyId, city.trim(), state.trim())
                .map(this::toResponse);
    }

    @Override
    public BranchResponse create(BranchRequest request) {
        if (request.address() == null || request.address().isBlank()) {
            throw new IllegalArgumentException("La dirección es obligatoria.");
        }
        Optional<Branch> existing = repository.findFirstByCompany_IdAndCityIgnoreCaseAndStateIgnoreCase(
                request.companyId(),
                request.city() != null ? request.city().trim() : "",
                request.state() != null ? request.state().trim() : "");
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        Branch b = new Branch();
        b.setCompany(companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + request.companyId())));
        b.setCity(request.city());
        b.setState(request.state());
        b.setAddress(request.address());
        b.setPhone(request.phone());
        b.setEmail(request.email());
        b.setContactPersonName(request.contactPersonName());
        b.setIsClient(request.isClient());
        b.setIsDistributor(request.isDistributor());
        b.setIsServiceCenter(request.isServiceCenter());
        return toResponse(repository.save(b));
    }

    @Override
    public BranchResponse update(Long id, BranchRequest request) {
        Branch b = findEntityById(id);
        securityScope.assertBranchReadable(b.getId());
        if (Role.isDistributorScoped(securityScope.currentUser().getRole())
                && clientRepository.existsByBranch_Id(b.getId())) {
            throw new IllegalArgumentException("client updates must be requested for review");
        }
        b.setCompany(companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + request.companyId())));
        b.setCity(request.city());
        b.setState(request.state());
        b.setAddress(request.address());
        b.setPhone(request.phone());
        b.setEmail(request.email());
        b.setContactPersonName(request.contactPersonName());
        b.setIsClient(request.isClient());
        b.setIsDistributor(request.isDistributor());
        b.setIsServiceCenter(request.isServiceCenter());
        return toResponse(repository.save(b));
    }

    @Override
    public void delete(Long id) {
        Branch b = findEntityById(id);
        securityScope.assertBranchReadable(b.getId());
        if (userRepository.existsByBranchId(id)) {
            throw new IllegalArgumentException("branch has linked users and cannot be deleted: " + id);
        }
        repository.delete(b);
    }

    private Branch findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    private BranchResponse toResponse(Branch b) {
        return new BranchResponse(
                b.getId(),
                b.getCompanyId(),
                b.getCity(),
                b.getState(),
                b.getAddress(),
                b.getPhone(),
                b.getEmail(),
                b.getContactPersonName(),
                b.getCreatedAt(),
                b.getIsClient(),
                b.getIsDistributor(),
                b.getIsServiceCenter());
    }
}
