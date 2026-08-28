package com.aeg.core.firmware.ota;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface OtaFirmwareService {

    /**
     * Returns the metadata JSON for the latest firmware registered under {@code modelCode}.
     * Validates {@code token} against the configured static download token.
     *
     * @throws org.springframework.web.server.ResponseStatusException with 401 on token mismatch.
     * @throws com.aeg.core.servicecenter.ResourceNotFoundException if no firmware is found.
     */
    OtaMetadataResponse getLatestMetadata(String modelCode, String token);

    /**
     * Streams the binary for the latest firmware registered under {@code modelCode}.
     * Validates {@code token} against the configured static download token.
     *
     * @throws org.springframework.web.server.ResponseStatusException with 401 on token mismatch.
     * @throws com.aeg.core.servicecenter.ResourceNotFoundException   if no firmware is found.
     */
    ResponseEntity<Resource> downloadLatest(String modelCode, String token);
}
