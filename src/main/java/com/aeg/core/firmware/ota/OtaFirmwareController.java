package com.aeg.core.firmware.ota;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints consumed by the ESP32 for OTA firmware updates.
 *
 * <p>These endpoints are permit-all in {@code SecurityConfig} and do NOT require a JWT.
 * The download endpoint is protected by a static token passed in the {@code x-auth-token} header.</p>
 */
@RestController
@RequestMapping("/ota")
public class OtaFirmwareController {

    private final OtaFirmwareService service;

    public OtaFirmwareController(OtaFirmwareService service) {
        this.service = service;
    }

    /**
     * Returns the metadata JSON for the latest firmware of the given model.
     *
     * <pre>
     * GET /ota/latest?model=AEG-R1
     * </pre>
     *
     * Response example:
     * <pre>
     * {
     *   "modelPtr": "AEG-R1",
     *   "hardVer": null,
     *   "firmVer": "1.1.0",
     *   "buildDate": 1787244453,
     *   "checksum": "56951bfa64338b2eeb5a68b7c64e272a",
     *   "fileSize": 1188512
     * }
     * </pre>
     */
    @GetMapping("/latest")
    public OtaMetadataResponse getLatest(@RequestParam String model) {
        return service.getLatestMetadata(model);
    }

    /**
     * Streams the latest firmware binary for the given model.
     *
     * <pre>
     * GET /ota/download?model=AEG-R1
     * Header: x-auth-token: &lt;TOKEN&gt;
     * </pre>
     *
     * <p>The ESP32 should compute the MD5 of the downloaded bytes and compare it with
     * the {@code checksum} field obtained from {@code /ota/latest}. If they match,
     * the device may proceed with the OTA installation.</p>
     *
     * <p>Returns {@code 401 Unauthorized} if the token is missing or incorrect.</p>
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam String model,
            @RequestHeader(value = "x-auth-token", required = false) String token) {
        return service.downloadLatest(model, token);
    }
}
