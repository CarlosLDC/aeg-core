package com.aeg.core.technician;

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

import com.aeg.core.technician.dto.TechnicianRequest;
import com.aeg.core.technician.dto.TechnicianResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

	private final TechnicianService service;

	public TechnicianController(TechnicianService service) {
		this.service = service;
	}

	@GetMapping
	public List<TechnicianResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public TechnicianResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TechnicianResponse create(@Valid @RequestBody TechnicianRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public TechnicianResponse update(@PathVariable Long id, @Valid @RequestBody TechnicianRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}
}
