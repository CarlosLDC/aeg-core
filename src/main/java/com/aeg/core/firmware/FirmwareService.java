package com.aeg.core.firmware;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.aeg.core.firmware.dto.FirmwareResponse;

public interface FirmwareService {

	List<FirmwareResponse> findAll(Long printerModelId);

	FirmwareResponse findById(Long id);

	FirmwareResponse create(MultipartFile file, String version, Long printerModelId, String notes);

	void delete(Long id);

	ResponseEntity<Resource> download(Long id);
}
