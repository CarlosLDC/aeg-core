package com.aeg.core.printer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

/**
 * Utility to generate the 16-character encryption key for printers.
 * Matches ESP32 firmware logic:
 * <pre>
 *   String input = String(nroRegEnc) + "|" + String(macAddrEnc);
 *   md5.begin();
 *   md5.add(input);
 *   md5.calculate();
 *   llaveEncriptada = md5.toString().substring(0, 16);
 * </pre>
 */
public final class PrinterEncryptionKeyGenerator {

    private PrinterEncryptionKeyGenerator() {
    }

    /**
     * Generates the 16-character hexadecimal encryption key from fiscal serial and MAC address.
     *
     * @param fiscalSerial registration number / serial fiscal (e.g. "GRA0000001")
     * @param macAddress MAC address (e.g. "3C:DC:75:64:97:C0")
     * @return 16 lowercase hex characters, or {@code null} if either argument is null or blank
     */
    public static String generateKey(String fiscalSerial, String macAddress) {
        if (fiscalSerial == null || fiscalSerial.isBlank() || macAddress == null || macAddress.isBlank()) {
            return null;
        }

        String normalizedSerial = fiscalSerial.trim().toUpperCase(Locale.ROOT);
        String normalizedMac = MacAddressNormalizer.toColonForm(macAddress);
        if (normalizedMac == null) {
            normalizedMac = macAddress.trim().toUpperCase(Locale.ROOT);
        }

        String input = normalizedSerial + "|" + normalizedMac;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }
}
