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
import com.aeg.core.firmware.dto.FirmwareUploadJobResponse;

@RestController
@RequestMapping("/api/firmwares")
public class FirmwareController {

	private final FirmwareService service;
	private final FirmwareUploadJobs uploadJobs;

	public FirmwareController(FirmwareService service, FirmwareUploadJobs uploadJobs) {
		this.service = service;
		this.uploadJobs = uploadJobs;
	}

	@GetMapping
	public List<FirmwareResponse> findAll(@RequestParam(required = false) Long printerModelId) {
		return service.findAll(printerModelId);
	}

	@GetMapping("/uploads/{jobId}")
	public FirmwareUploadJobResponse getUploadJob(@PathVariable String jobId) {
		return uploadJobs.get(jobId);
	}

	@GetMapping("/{id}")
	public FirmwareResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	/**
	 * Accepts the multipart quickly and runs SFTP+DB in the background so gateways
	 * do not return 504 while the droplet transfer is in progress.
	 */
	@PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FirmwareUploadJobResponse> create(
			@RequestParam("file") MultipartFile file,
			@RequestParam("version") String version,
			@RequestParam(value = "printerModelId", required = false) Long printerModelId,
			@RequestParam(value = "notes", required = false) String notes) {
		FirmwareUploadJobResponse job = uploadJobs.enqueue(file, version, printerModelId, notes);
		return ResponseEntity.accepted().body(job);
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
