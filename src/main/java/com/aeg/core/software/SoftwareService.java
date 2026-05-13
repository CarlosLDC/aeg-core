package com.aeg.core.software;

import java.util.List;

import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;

public interface SoftwareService {
    List<SoftwareResponse> findAll();
    SoftwareResponse findById(Long id);
    SoftwareResponse create(SoftwareRequest request);
    SoftwareResponse update(Long id, SoftwareRequest request);
    void delete(Long id);
}
