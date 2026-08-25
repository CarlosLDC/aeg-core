package com.aeg.core.firmware;

/**
 * In-memory payload for an async firmware create after validation.
 */
public record PreparedFirmwareUpload(
		byte[] bytes,
		String fileName,
		String version,
		Long printerModelId,
		String notes,
		String checksumSha256,
		String checksumMd5) {
}
