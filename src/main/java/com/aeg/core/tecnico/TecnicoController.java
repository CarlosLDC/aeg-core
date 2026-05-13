package com.aeg.core.tecnico;

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

import com.aeg.core.tecnico.dto.TecnicoRequest;
import com.aeg.core.tecnico.dto.TecnicoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/technicians")
public class TecnicoController {

	private final TecnicoService service;

	public TecnicoController(TecnicoService service) {
		this.service = service;
	}

	@GetMapping
	public List<TecnicoResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public TecnicoResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TecnicoResponse create(@Valid @RequestBody TecnicoRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public TecnicoResponse update(@PathVariable Long id, @Valid @RequestBody TecnicoRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
