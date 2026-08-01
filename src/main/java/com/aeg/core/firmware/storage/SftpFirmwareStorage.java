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
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SftpFirmwareStorage implements FirmwareStorage {

	/** Fail fast so App Platform returns 503 instead of gateway 504 on unreachable hosts. */
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(15);
	private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(60);

	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final String remoteDir;

	public SftpFirmwareStorage(
			@Value("${app.firmware.sftp.host:206.189.231.128}") String host,
			@Value("${app.firmware.sftp.port:2222}") int port,
			@Value("${app.firmware.sftp.username:}") String username,
			@Value("${app.firmware.sftp.password:}") String password,
			@Value("${app.firmware.sftp.remote-dir:/var/www/firmware}") String remoteDir) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.remoteDir = trimTrailingSlash(remoteDir);
		log.info("Firmware SFTP configured host={} port={} remoteDir={}", this.host, this.port, this.remoteDir);
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
			String remotePath = remotePath(fileName);
			try {
				sftp.remove(remotePath);
			} catch (IOException e) {
				if (isNoSuchFile(e)) {
					log.info("Firmware remote file already absent, treating delete as success: {}", remotePath);
					return null;
				}
				throw e;
			}
			return null;
		});
	}

	private static boolean isNoSuchFile(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof java.nio.file.NoSuchFileException) {
				return true;
			}
			if (t instanceof SftpException sftpEx
					&& sftpEx.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
				return true;
			}
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
			if (msg.contains("no such file") || msg.contains("not found") || msg.contains("does not exist")) {
				return true;
			}
		}
		return false;
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
			CoreModuleProperties.IDLE_TIMEOUT.set(client, IDLE_TIMEOUT);
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
			String hint;
			if (port == 22) {
				hint = " DigitalOcean App Platform blocks outbound TCP 22; set sshd + FIRMWARE_SFTP_PORT to a non-default port (e.g. 2222).";
			} else if (messageLooksLikeConnectionRefused(e)) {
				hint = " Nothing is accepting TCP " + port + " on " + host
						+ ". On the droplet run scripts/setup-firmware-sftp-droplet.sh (sshd Port "
						+ port + ") and open the firewall.";
			} else if (messageLooksLikeTimeoutOrUnreachable(e)) {
				hint = " Host " + host + ":" + port
						+ " is unreachable from App Platform. Use the droplet public IP unless the app is attached to the same VPC.";
			} else {
				hint = "";
			}
			throw new IllegalStateException("Firmware SFTP operation failed: " + e.getMessage() + hint, e);
		}
	}

	private static boolean messageLooksLikeConnectionRefused(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			String msg = t.getMessage();
			if (msg != null && msg.toLowerCase().contains("connection refused")) {
				return true;
			}
		}
		return false;
	}

	private static boolean messageLooksLikeTimeoutOrUnreachable(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
			if (msg.contains("timed out") || msg.contains("timeout") || msg.contains("unreachable")
					|| msg.contains("no route to host")) {
				return true;
			}
		}
		return false;
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
