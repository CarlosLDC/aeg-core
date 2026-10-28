package com.aeg.core.software;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.software.dto.SoftwareDistributorResponse;
import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class SoftwareServiceImpl implements SoftwareService {

    private final SoftwareRepository repository;
    private final CompanyRepository companyRepository;

    public SoftwareServiceImpl(SoftwareRepository repository, CompanyRepository companyRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SoftwareResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Override
    public SoftwareResponse create(SoftwareRequest request) {
        Software s = new Software();
        s.setName(request.name());
        s.setVersion(request.version());
        s.setProgrammingLanguages(request.programmingLanguages() == null ? java.util.Collections.emptyList() : request.programmingLanguages());
        s.setOperatingSystems(request.operatingSystems() == null ? java.util.Collections.emptyList() : request.operatingSystems());
        applyDeveloper(s, request);
        applyDistributors(s, request.distributorCompanyIds());
        return toResponse(repository.save(s));
    }

    @Override
    public SoftwareResponse update(Long id, SoftwareRequest request) {
        Software s = findEntityById(id);
        s.setName(request.name());
        s.setVersion(request.version());
        s.setProgrammingLanguages(request.programmingLanguages() == null ? java.util.Collections.emptyList() : request.programmingLanguages());
        s.setOperatingSystems(request.operatingSystems() == null ? java.util.Collections.emptyList() : request.operatingSystems());
        applyDeveloper(s, request);
        if (request.distributorCompanyIds() != null) {
            applyDistributors(s, request.distributorCompanyIds());
        }
        return toResponse(repository.save(s));
    }

    @Override
    public void delete(Long id) {
        Software s = findEntityById(id);
        repository.delete(s);
    }

    @Override
    public SoftwareResponse addDistributor(Long softwareId, Long companyId) {
        Software s = findEntityById(softwareId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
        if (s.getDistributors().stream().noneMatch(c -> c.getId().equals(companyId))) {
            s.getDistributors().add(company);
            repository.save(s);
        }
        return toResponse(s);
    }

    @Override
    public SoftwareResponse removeDistributor(Long softwareId, Long companyId) {
        Software s = findEntityById(softwareId);
        s.getDistributors().removeIf(c -> c.getId().equals(companyId));
        repository.save(s);
        return toResponse(s);
    }

    private void applyDeveloper(Software s, SoftwareRequest request) {
        if (request.developerCompanyId() == null) {
            s.setDeveloperCompany(null);
        } else {
            s.setDeveloperCompany(companyRepository.findById(request.developerCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Company not found with id: " + request.developerCompanyId())));
        }
        s.setExternal(Boolean.TRUE.equals(request.external()));
    }

    private void applyDistributors(Software s, List<Long> distributorCompanyIds) {
        if (distributorCompanyIds == null || distributorCompanyIds.isEmpty()) {
            s.getDistributors().clear();
            return;
        }
        List<Company> companies = companyRepository.findAllById(distributorCompanyIds);
        s.setDistributors(new ArrayList<>(companies));
    }

    private Software findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Software not found with id: " + id));
    }

    private SoftwareResponse toResponse(Software s) {
        List<String> programmingLanguages = s.getProgrammingLanguages() == null
                ? List.of()
                : List.copyOf(s.getProgrammingLanguages());
        List<String> operatingSystems = s.getOperatingSystems() == null
                ? List.of()
                : List.copyOf(s.getOperatingSystems());
        List<SoftwareDistributorResponse> distributors = s.getDistributors() == null
                ? List.of()
                : s.getDistributors().stream()
                        .map(c -> new SoftwareDistributorResponse(c.getId(), c.getRif(), c.getBusinessName()))
                        .toList();
        return new SoftwareResponse(
                s.getId(),
                s.getName(),
                s.getVersion(),
                s.getCreatedAt(),
                programmingLanguages,
                operatingSystems,
                s.getDeveloperCompany() == null ? null : s.getDeveloperCompany().getId(),
                s.getDeveloperCompany() == null ? null : s.getDeveloperCompany().getRif(),
                s.getDeveloperCompany() == null ? null : s.getDeveloperCompany().getBusinessName(),
                s.isExternal(),
                distributors);
    }
}
