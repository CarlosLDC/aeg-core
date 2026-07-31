package com.aeg.core.firmware;

import java.util.Locale;
import java.util.regex.Pattern;

final class FirmwareFileNames {

	private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9._-]+$");

	private FirmwareFileNames() {
	}

	static String sanitize(String originalFileName) {
		if (originalFileName == null || originalFileName.isBlank()) {
			throw new IllegalArgumentException("file name is required");
		}
		String name = originalFileName.trim().replace('\\', '/');
		int slash = name.lastIndexOf('/');
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		if (name.isBlank()) {
			throw new IllegalArgumentException("file name is required");
		}
		if (!name.toLowerCase(Locale.ROOT).endsWith(".bin")) {
			throw new IllegalArgumentException("file must have .bin extension");
		}
		if (!SAFE_NAME.matcher(name).matches()) {
			throw new IllegalArgumentException(
					"file name may only contain letters, digits, '.', '_' and '-'");
		}
		return name;
	}

	static String buildPublicUrl(String publicBaseUrl, String fileName) {
		String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/" + fileName;
	}
}
