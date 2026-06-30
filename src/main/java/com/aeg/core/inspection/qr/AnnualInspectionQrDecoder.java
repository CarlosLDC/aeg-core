package com.aeg.core.inspection.qr;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class AnnualInspectionQrDecoder {

    private static final String CIPHER_TRANSFORMATION = "AES/ECB/NoPadding";
    private static final Pattern QR_QUERY_PARAM_PATTERN =
            Pattern.compile("(?:^|[?&#])(qrCodigo|qr|code)=([^&#]+)", Pattern.CASE_INSENSITIVE);

    private final AnnualInspectionQrSettings settings;

    public AnnualInspectionQrDecoder(AnnualInspectionQrSettings settings) {
        this.settings = settings;
    }

    public AnnualInspectionQrPayload decode(String qrCodigo) {
        if (qrCodigo == null || qrCodigo.isBlank()) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        String secret = settings.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(AnnualInspectionQrMessages.INVALID_CODE);
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16) {
            throw new IllegalStateException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        byte[] cipherBytes;
        try {
            cipherBytes = decodeBase64(normalizeQrInput(qrCodigo));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }
        if (cipherBytes.length == 0 || cipherBytes.length % 16 != 0) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        byte[] decrypted;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));
            decrypted = cipher.doFinal(cipherBytes);
        } catch (Exception ex) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        String plaintext = stripTrailingNulls(decrypted);
        if (plaintext.isBlank()) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        String[] parts = plaintext.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        String registro = parts[0].trim();
        String mac = parts[1].trim();
        String fecha = parts[2].trim();
        if (registro.isEmpty() || mac.isEmpty() || fecha.isEmpty()) {
            throw new IllegalArgumentException(AnnualInspectionQrMessages.INVALID_CODE);
        }

        return new AnnualInspectionQrPayload(registro, mac, fecha);
    }

    public String normalizeQrCodigo(String qrCodigo) {
        if (qrCodigo == null) {
            return "";
        }
        return normalizeQrInput(qrCodigo).replaceAll("\\s+", "");
    }

    private static byte[] decodeBase64(String value) {
        IllegalArgumentException lastError = null;
        for (String candidate : base64Candidates(value)) {
            try {
                return Base64.getDecoder().decode(candidate);
            } catch (IllegalArgumentException ex) {
                lastError = ex;
            }
            try {
                return Base64.getUrlDecoder().decode(candidate);
            } catch (IllegalArgumentException ex) {
                lastError = ex;
            }
            try {
                return Base64.getMimeDecoder().decode(candidate);
            } catch (IllegalArgumentException ex) {
                lastError = ex;
            }
        }
        throw lastError != null ? lastError : new IllegalArgumentException("invalid base64");
    }

    private static String[] base64Candidates(String value) {
        String trimmed = stripWrappingQuotes(value.trim()).replaceAll("\\s+", "");
        String standard = padBase64(trimmed);
        String urlSafe = padBase64(trimmed.replace('+', '-').replace('/', '_'));
        String standardFromUrlSafe = padBase64(trimmed.replace('-', '+').replace('_', '/'));
        return new String[] { standard, urlSafe, standardFromUrlSafe };
    }

    private static String normalizeQrInput(String qrCodigo) {
        String trimmed = qrCodigo.trim();
        Optional<String> queryParam = extractQrQueryParam(trimmed);
        return queryParam.orElse(trimmed);
    }

    private static Optional<String> extractQrQueryParam(String value) {
        Matcher matcher = QR_QUERY_PARAM_PATTERN.matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(percentDecodePreservingPlus(matcher.group(2)));
    }

    private static String percentDecodePreservingPlus(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '%' && i + 2 < value.length()) {
                String hex = value.substring(i + 1, i + 3);
                try {
                    out.append((char) Integer.parseInt(hex, 16));
                    i += 2;
                    continue;
                } catch (NumberFormatException ignored) {
                    /* keep literal percent below */
                }
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        if (remainder == 1) {
            return value;
        }
        return value + "=".repeat(4 - remainder);
    }

    private static String stripTrailingNulls(byte[] bytes) {
        int end = bytes.length;
        while (end > 0 && bytes[end - 1] == 0) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
