package com.aeg.core.software;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class SoftwareServiceImpl implements SoftwareService {

    private final SoftwareRepository repository;

    public SoftwareServiceImpl(SoftwareRepository repository) {
        this.repository = repository;
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
        return toResponse(repository.save(s));
    }

    @Override
    public SoftwareResponse update(Long id, SoftwareRequest request) {
        Software s = findEntityById(id);
        s.setName(request.name());
        s.setVersion(request.version());
        s.setProgrammingLanguages(request.programmingLanguages() == null ? java.util.Collections.emptyList() : request.programmingLanguages());
        s.setOperatingSystems(request.operatingSystems() == null ? java.util.Collections.emptyList() : request.operatingSystems());
        return toResponse(repository.save(s));
    }

    @Override
    public void delete(Long id) {
        Software s = findEntityById(id);
        repository.delete(s);
    }

    private Software findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Software not found with id: " + id));
    }

    private SoftwareResponse toResponse(Software s) {
        return new SoftwareResponse(s.getId(), s.getName(), s.getVersion(), s.getCreatedAt(), s.getProgrammingLanguages(), s.getOperatingSystems());
    }
}
