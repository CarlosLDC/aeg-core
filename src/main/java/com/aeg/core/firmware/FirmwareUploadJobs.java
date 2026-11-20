package com.aeg.core.firmware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

	private static final Duration JOB_TTL = Duration.ofMinutes(30);
	private static final int MAX_JOBS = 100;

	private record TrackedJob(FirmwareUploadJobResponse response, Instant updatedAt) {
		boolean isTerminal() {
			return response.status() == FirmwareUploadJobStatus.SUCCEEDED
					|| response.status() == FirmwareUploadJobStatus.FAILED;
		}
	}

	private final FirmwareService firmwareService;
	private final Map<String, TrackedJob> jobs = new ConcurrentHashMap<>();
	private final ThreadPoolExecutor executor = createExecutor();

	public FirmwareUploadJobs(FirmwareService firmwareService) {
		this.firmwareService = firmwareService;
	}

	private static ThreadPoolExecutor createExecutor() {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(
				2,
				2,
				60L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(),
				r -> {
					Thread t = new Thread(r, "firmware-upload");
					t.setDaemon(true);
					return t;
				});
		pool.allowCoreThreadTimeOut(true);
		return pool;
	}

	public FirmwareUploadJobResponse enqueue(
			MultipartFile file,
			String version,
			Long printerModelId,
			String notes) {
		evictExpiredJobs();
		// Validate + spool to temp file before the request ends (Multipart temp files disappear after).
		PreparedFirmwareUpload prepared = firmwareService.prepareCreate(file, version, printerModelId, notes);

		String jobId = UUID.randomUUID().toString();
		FirmwareUploadJobResponse pending = new FirmwareUploadJobResponse(
				jobId,
				FirmwareUploadJobStatus.PENDING,
				null,
				null);
		jobs.put(jobId, new TrackedJob(pending, Instant.now()));

		try {
			executor.execute(() -> {
				try {
					FirmwareResponse result = firmwareService.completeCreate(prepared);
					jobs.put(jobId, new TrackedJob(
							new FirmwareUploadJobResponse(
									jobId,
									FirmwareUploadJobStatus.SUCCEEDED,
									null,
									result),
							Instant.now()));
				} catch (Exception e) {
					log.error("Firmware upload job {} failed: {}", jobId, e.getMessage(), e);
					String message = e.getMessage() != null && !e.getMessage().isBlank()
							? e.getMessage()
							: "Firmware upload failed";
					jobs.put(jobId, new TrackedJob(
							new FirmwareUploadJobResponse(
									jobId,
									FirmwareUploadJobStatus.FAILED,
									message,
									null),
							Instant.now()));
				}
			});
		} catch (RuntimeException ex) {
			jobs.remove(jobId);
			deleteTempQuietly(prepared.tempFile());
			throw ex;
		}

		return pending;
	}

	public FirmwareUploadJobResponse get(String jobId) {
		evictExpiredJobs();
		TrackedJob tracked = jobs.get(jobId);
		if (tracked == null) {
			throw new com.aeg.core.servicecenter.ResourceNotFoundException(
					"Firmware upload job not found with id: " + jobId);
		}
		return tracked.response();
	}

	private void evictExpiredJobs() {
		Instant cutoff = Instant.now().minus(JOB_TTL);
		jobs.entrySet().removeIf(entry ->
				entry.getValue().isTerminal() && entry.getValue().updatedAt().isBefore(cutoff));

		int excess = jobs.size() - MAX_JOBS;
		if (excess <= 0) {
			return;
		}
		jobs.entrySet().stream()
				.filter(entry -> entry.getValue().isTerminal())
				.sorted(Comparator.comparing(entry -> entry.getValue().updatedAt()))
				.limit(excess)
				.map(Map.Entry::getKey)
				.toList()
				.forEach(jobs::remove);
	}

	private static void deleteTempQuietly(Path tempFile) {
		if (tempFile == null) {
			return;
		}
		try {
			Files.deleteIfExists(tempFile);
		} catch (IOException ex) {
			log.debug("Failed to delete temporary firmware file {}: {}", tempFile, ex.getMessage());
		}
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}
}
