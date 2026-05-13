package com.aeg.core.tecnico;

import java.util.List;

import com.aeg.core.tecnico.dto.TecnicoRequest;
import com.aeg.core.tecnico.dto.TecnicoResponse;

public interface TecnicoService {

	List<TecnicoResponse> findAll();

	TecnicoResponse findById(Long id);

	TecnicoResponse create(TecnicoRequest request);

	TecnicoResponse update(Long id, TecnicoRequest request);

	void delete(Long id);
}
