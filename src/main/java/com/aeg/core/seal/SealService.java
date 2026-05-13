package com.aeg.core.seal;

import java.util.List;

import com.aeg.core.seal.dto.SealRequest;
import com.aeg.core.seal.dto.SealResponse;

public interface SealService {
    List<SealResponse> findAll();
    SealResponse findById(Long id);
    SealResponse create(SealRequest request);
    SealResponse update(Long id, SealRequest request);
    void delete(Long id);
}
