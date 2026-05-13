package com.aeg.core.serviciotecnico;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.serviciotecnico.dto.ServicioTecnicoRequest;
import com.aeg.core.serviciotecnico.dto.ServicioTecnicoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/technical-services")
public class ServicioTecnicoController {

	private final ServicioTecnicoService service;

	public ServicioTecnicoController(ServicioTecnicoService service) {
		this.service = service;
	}

	@GetMapping
	public List<ServicioTecnicoResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public ServicioTecnicoResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServicioTecnicoResponse create(@Valid @RequestBody ServicioTecnicoRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public ServicioTecnicoResponse update(
			@PathVariable Long id,
			@Valid @RequestBody ServicioTecnicoRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
