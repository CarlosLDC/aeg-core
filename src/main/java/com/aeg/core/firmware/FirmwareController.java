package com.aeg.core.firmware;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aeg.core.firmware.dto.FirmwareResponse;

@RestController
@RequestMapping("/api/firmwares")
public class FirmwareController {

	private final FirmwareService service;

	public FirmwareController(FirmwareService service) {
		this.service = service;
	}

	@GetMapping
	public List<FirmwareResponse> findAll(@RequestParam(required = false) Long printerModelId) {
		return service.findAll(printerModelId);
	}

	@GetMapping("/{id}")
	public FirmwareResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public FirmwareResponse create(
			@RequestParam("file") MultipartFile file,
			@RequestParam("version") String version,
			@RequestParam(value = "printerModelId", required = false) Long printerModelId,
			@RequestParam(value = "notes", required = false) String notes) {
		return service.create(file, version, printerModelId, notes);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<Resource> download(@PathVariable Long id) {
		return service.download(id);
	}
}
