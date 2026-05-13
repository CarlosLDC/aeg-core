package com.aeg.core.inspeccion;

import java.util.List;

import com.aeg.core.inspeccion.dto.InspeccionAnualRequest;
import com.aeg.core.inspeccion.dto.InspeccionAnualResponse;

public interface InspeccionAnualService {

	List<InspeccionAnualResponse> findAll();

	InspeccionAnualResponse findById(Long id);

	InspeccionAnualResponse create(InspeccionAnualRequest request);

	InspeccionAnualResponse update(Long id, InspeccionAnualRequest request);

	void delete(Long id);
}
