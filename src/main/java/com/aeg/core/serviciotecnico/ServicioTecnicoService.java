package com.aeg.core.serviciotecnico;

import java.util.List;

import com.aeg.core.serviciotecnico.dto.ServicioTecnicoRequest;
import com.aeg.core.serviciotecnico.dto.ServicioTecnicoResponse;

public interface ServicioTecnicoService {

	List<ServicioTecnicoResponse> findAll();

	ServicioTecnicoResponse findById(Long id);

	ServicioTecnicoResponse create(ServicioTecnicoRequest request);

	ServicioTecnicoResponse update(Long id, ServicioTecnicoRequest request);

	void delete(Long id);
}
