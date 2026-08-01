package com.aeg.core.firmware;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.aeg.core.firmware.dto.FirmwareResponse;
import com.aeg.core.firmware.dto.FirmwareUpdateRequest;

public interface FirmwareService {

	List<FirmwareResponse> findAll(Long printerModelId);

	FirmwareResponse findById(Long id);

	/** Validates input and buffers bytes for a later {@link #completeCreate}. */
	PreparedFirmwareUpload prepareCreate(MultipartFile file, String version, Long printerModelId, String notes);

	/** SFTP upload + DB persist (runs on upload worker thread). */
	FirmwareResponse completeCreate(PreparedFirmwareUpload prepared);

	/** Sync create (tests / internal). */
	FirmwareResponse create(MultipartFile file, String version, Long printerModelId, String notes);

	/** Updates metadata only (version, model, notes). Binary stays on SFTP. */
	FirmwareResponse update(Long id, FirmwareUpdateRequest request);

	void delete(Long id);

	ResponseEntity<Resource> download(Long id);
}
