package com.aeg.core.firmware.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SftpFirmwareStorage implements FirmwareStorage {

	/**
	 * WARNING(TEMP): hardcode de la IP pública del droplet.
	 * Eliminar cuando FIRMWARE_SFTP_HOST en DigitalOcean App Platform sea 206.189.231.128
	 * (hoy el env sigue en 10.116.0.4 y no es alcanzable desde App Platform → 504).
	 */
	private static final String TEMP_FORCE_PUBLIC_SFTP_HOST = "206.189.231.128";

	/** Fail fast so App Platform returns 503 instead of gateway 504 on unreachable hosts. */
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(15);

	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final String remoteDir;

	public SftpFirmwareStorage(
			@Value("${app.firmware.sftp.host}") String host,
			@Value("${app.firmware.sftp.port:22}") int port,
			@Value("${app.firmware.sftp.username:}") String username,
			@Value("${app.firmware.sftp.password:}") String password,
			@Value("${app.firmware.sftp.remote-dir:/var/www/firmware}") String remoteDir) {
		// WARNING(TEMP): ignora FIRMWARE_SFTP_HOST / app.firmware.sftp.host hasta corregir el env en DO.
		if (!TEMP_FORCE_PUBLIC_SFTP_HOST.equals(host)) {
			log.warn(
					"WARNING(TEMP): overriding firmware SFTP host '{}' with hardcoded public IP {}. Remove TEMP_FORCE_PUBLIC_SFTP_HOST when DO env is fixed.",
					host,
					TEMP_FORCE_PUBLIC_SFTP_HOST);
		}
		this.host = TEMP_FORCE_PUBLIC_SFTP_HOST;
		this.port = port;
		this.username = username;
		this.password = password;
		this.remoteDir = trimTrailingSlash(remoteDir);
	}

	@Override
	public void upload(String fileName, InputStream content, long sizeBytes) {
		withSftp(sftp -> {
			String remotePath = remotePath(fileName);
			try (OutputStream out = sftp.write(remotePath)) {
				content.transferTo(out);
			}
			return null;
		});
	}

	@Override
	public byte[] download(String fileName) {
		return withSftp(sftp -> {
			try (InputStream in = sftp.read(remotePath(fileName))) {
				return in.readAllBytes();
			}
		});
	}

	@Override
	public void delete(String fileName) {
		withSftp(sftp -> {
			sftp.remove(remotePath(fileName));
			return null;
		});
	}

	private String remotePath(String fileName) {
		return remoteDir + "/" + fileName;
	}

	private void ensureConfigured() {
		if (!StringUtils.hasText(host) || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
			throw new IllegalStateException(
					"Firmware SFTP is not configured (FIRMWARE_SFTP_HOST / FIRMWARE_SFTP_USER / FIRMWARE_SFTP_PASSWORD).");
		}
	}

	private <T> T withSftp(SftpCallback<T> callback) {
		ensureConfigured();
		try (SshClient client = SshClient.setUpDefaultClient()) {
			// NIO2 socket connect defaults to infinite; without this, unreachable hosts
			// hang until the App Platform gateway returns 504.
			CoreModuleProperties.IO_CONNECT_TIMEOUT.set(client, CONNECT_TIMEOUT);
			client.start();
			try (ClientSession session = client.connect(username, host, port)
					.verify(CONNECT_TIMEOUT)
					.getSession()) {
				session.addPasswordIdentity(password);
				session.auth().verify(AUTH_TIMEOUT);
				try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
					return callback.apply(sftp);
				}
			}
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			log.error("Firmware SFTP failed host={} port={} remoteDir={}: {}", host, port, remoteDir, e.getMessage());
			throw new IllegalStateException("Firmware SFTP operation failed: " + e.getMessage(), e);
		}
	}

	private static String trimTrailingSlash(String path) {
		if (path == null || path.isBlank()) {
			return "/var/www/firmware";
		}
		String asString = path.trim().replace('\\', '/');
		while (asString.endsWith("/") && asString.length() > 1) {
			asString = asString.substring(0, asString.length() - 1);
		}
		return asString;
	}

	@FunctionalInterface
	private interface SftpCallback<T> {
		T apply(SftpClient sftp) throws IOException;
	}
}
