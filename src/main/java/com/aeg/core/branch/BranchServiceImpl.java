package com.aeg.core.branch;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.dto.BranchRequest;
import com.aeg.core.branch.dto.BranchResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository repository;
    private final com.aeg.core.company.CompanyRepository companyRepository;

    public BranchServiceImpl(BranchRepository repository, com.aeg.core.company.CompanyRepository companyRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Override
    public BranchResponse create(BranchRequest request) {
        Branch b = new Branch();
        b.setCompany(companyRepository.getReferenceById(request.companyId()));
        b.setCity(request.city());
        b.setState(request.state());
        b.setAddress(request.address());
        b.setPhone(request.phone());
        b.setEmail(request.email());
        b.setIsClient(request.isClient());
        b.setIsDistributor(request.isDistributor());
        b.setIsServiceCenter(request.isServiceCenter());
        return toResponse(repository.save(b));
    }

    @Override
    public BranchResponse update(Long id, BranchRequest request) {
        Branch b = findEntityById(id);
        b.setCompany(companyRepository.getReferenceById(request.companyId()));
        b.setCity(request.city());
        b.setState(request.state());
        b.setAddress(request.address());
        b.setPhone(request.phone());
        b.setEmail(request.email());
        b.setIsClient(request.isClient());
        b.setIsDistributor(request.isDistributor());
        b.setIsServiceCenter(request.isServiceCenter());
        return toResponse(repository.save(b));
    }

    @Override
    public void delete(Long id) {
        Branch b = findEntityById(id);
        repository.delete(b);
    }

    private Branch findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    private BranchResponse toResponse(Branch b) {
        return new BranchResponse(b.getId(), b.getCompanyId(), b.getCity(), b.getState(), b.getAddress(), b.getPhone(), b.getEmail(), b.getCreatedAt(), b.getIsClient(), b.getIsDistributor(), b.getIsServiceCenter());
    }
}
