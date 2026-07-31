package com.aeg.core.firmware.dto;

import java.time.OffsetDateTime;

public record FirmwareResponse(
		Long id,
		String version,
		String fileName,
		Long sizeBytes,
		String checksumSha256,
		Long printerModelId,
		String notes,
		String downloadUrl,
		OffsetDateTime createdAt
) {
}
