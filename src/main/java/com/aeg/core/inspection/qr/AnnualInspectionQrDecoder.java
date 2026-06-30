package com.aeg.core.inspection.qr;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class AnnualInspectionQrDecoder {

    private static final String CIPHER_TRANSFORMATION = "AES/ECB/NoPadding";

    private final AnnualInspectionQrSettings settings;

    public AnnualInspectionQrDecoder(AnnualInspectionQrSettings settings) {
        this.settings = settings;
    }

    public AnnualInspectionQrPayload decode(String qrCodigo) {
        if (qrCodigo == null || qrCodigo.isBlank()) {
            throw new IllegalArgumentException("El código QR es obligatorio.");
        }

        String secret = settings.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("La clave de desencriptación del QR no está configurada.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16) {
            throw new IllegalStateException("La clave de desencriptación del QR debe tener exactamente 16 bytes UTF-8.");
        }

        byte[] cipherBytes;
        try {
            cipherBytes = Base64.getDecoder().decode(qrCodigo.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El código QR no es Base64 válido.");
        }
        if (cipherBytes.length == 0 || cipherBytes.length % 16 != 0) {
            throw new IllegalArgumentException("El código QR no tiene un tamaño de bloque AES válido.");
        }

        byte[] decrypted;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));
            decrypted = cipher.doFinal(cipherBytes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo desencriptar el código QR.");
        }

        String plaintext = stripTrailingNulls(decrypted);
        if (plaintext.isBlank()) {
            throw new IllegalArgumentException("El código QR desencriptado está vacío.");
        }

        String[] parts = plaintext.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "El código QR desencriptado no tiene el formato registro|mac|fecha.");
        }

        String registro = parts[0].trim();
        String mac = parts[1].trim();
        String fecha = parts[2].trim();
        if (registro.isEmpty() || mac.isEmpty() || fecha.isEmpty()) {
            throw new IllegalArgumentException("El código QR desencriptado contiene campos vacíos.");
        }

        return new AnnualInspectionQrPayload(registro, mac, fecha);
    }

    private static String stripTrailingNulls(byte[] bytes) {
        int end = bytes.length;
        while (end > 0 && bytes[end - 1] == 0) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
