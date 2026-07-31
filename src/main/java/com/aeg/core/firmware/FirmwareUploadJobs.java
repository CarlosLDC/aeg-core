package com.aeg.core.firmware;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.aeg.core.firmware.dto.FirmwareResponse;
import com.aeg.core.firmware.dto.FirmwareUploadJobResponse;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Queues firmware SFTP+DB work so the HTTP request can return before App Platform /
 * Cloudflare gateway timeouts (504) kill the upload.
 */
@Component
@Slf4j
public class FirmwareUploadJobs {

	private final FirmwareService firmwareService;
	private final Map<String, FirmwareUploadJobResponse> jobs = new ConcurrentHashMap<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "firmware-upload");
		t.setDaemon(true);
		return t;
	});

	public FirmwareUploadJobs(FirmwareService firmwareService) {
		this.firmwareService = firmwareService;
	}

	public FirmwareUploadJobResponse enqueue(
			MultipartFile file,
			String version,
			Long printerModelId,
			String notes) {
		// Validate + buffer before the request ends (Multipart temp files disappear after).
		PreparedFirmwareUpload prepared = firmwareService.prepareCreate(file, version, printerModelId, notes);

		String jobId = UUID.randomUUID().toString();
		FirmwareUploadJobResponse pending = new FirmwareUploadJobResponse(
				jobId,
				FirmwareUploadJobStatus.PENDING,
				null,
				null);
		jobs.put(jobId, pending);

		executor.execute(() -> {
			try {
				FirmwareResponse result = firmwareService.completeCreate(prepared);
				jobs.put(jobId, new FirmwareUploadJobResponse(
						jobId,
						FirmwareUploadJobStatus.SUCCEEDED,
						null,
						result));
			} catch (Exception e) {
				log.error("Firmware upload job {} failed: {}", jobId, e.getMessage(), e);
				String message = e.getMessage() != null && !e.getMessage().isBlank()
						? e.getMessage()
						: "Firmware upload failed";
				jobs.put(jobId, new FirmwareUploadJobResponse(
						jobId,
						FirmwareUploadJobStatus.FAILED,
						message,
						null));
			}
		});

		return pending;
	}

	public FirmwareUploadJobResponse get(String jobId) {
		FirmwareUploadJobResponse job = jobs.get(jobId);
		if (job == null) {
			throw new com.aeg.core.servicecenter.ResourceNotFoundException(
					"Firmware upload job not found with id: " + jobId);
		}
		return job;
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}
}
