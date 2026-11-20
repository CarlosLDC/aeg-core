package com.aeg.core.firmware;

import java.nio.file.Path;

/**
 * Spool-backed payload for an async firmware create after validation.
 */
public record PreparedFirmwareUpload(
		Path tempFile,
		long sizeBytes,
		String fileName,
		String version,
		Long printerModelId,
		String notes,
		String checksumSha256,
		String checksumMd5) {
}
