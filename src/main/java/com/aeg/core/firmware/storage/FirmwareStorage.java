package com.aeg.core.firmware.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Remote binary storage for firmware files (droplet via SFTP).
 */
public interface FirmwareStorage {

	record FirmwareDownload(InputStream stream, long contentLength) {
	}

	void upload(String fileName, InputStream content, long sizeBytes);

	byte[] download(String fileName);

	default FirmwareDownload openDownload(String fileName) {
		return null;
	}

	static FirmwareDownload openDownloadOrFallback(FirmwareStorage storage, String fileName) {
		FirmwareDownload streaming = storage.openDownload(fileName);
		if (streaming != null) {
			return streaming;
		}
		byte[] bytes = storage.download(fileName);
		return new FirmwareDownload(new ByteArrayInputStream(bytes), bytes.length);
	}

	void delete(String fileName);
}
