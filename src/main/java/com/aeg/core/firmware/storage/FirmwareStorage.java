package com.aeg.core.firmware.storage;

import java.io.InputStream;

/**
 * Remote binary storage for firmware files (droplet via SFTP).
 */
public interface FirmwareStorage {

	void upload(String fileName, InputStream content, long sizeBytes);

	byte[] download(String fileName);

	void delete(String fileName);
}
