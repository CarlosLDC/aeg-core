package com.aeg.core.enajenacion.mqtt;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MacAddressNormalizer {

    private static final Pattern COMPACT_MAC = Pattern.compile("^[0-9A-Fa-f]{12}$");
    private static final Pattern COLON_MAC = Pattern.compile(
            "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");

    private MacAddressNormalizer() {
    }

    public static String toColonForm(String mac) {
        if (mac == null || mac.isBlank()) {
            return null;
        }
        String trimmed = mac.trim();
        if (COLON_MAC.matcher(trimmed).matches()) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        String compact = trimmed.replace(":", "").toUpperCase(Locale.ROOT);
        if (!COMPACT_MAC.matcher(compact).matches()) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return compact.replaceAll("(.{2})(?=.)", "$1:").toUpperCase(Locale.ROOT);
    }

    public static String toCompactForm(String mac) {
        if (mac == null || mac.isBlank()) {
            return null;
        }
        return mac.replace(":", "").toUpperCase(Locale.ROOT);
    }

    public static String requireCompactForm(String mac) {
        String compact = toCompactForm(mac);
        if (compact == null || !COMPACT_MAC.matcher(compact).matches()) {
            throw new IllegalArgumentException("La dirección MAC no es válida.");
        }
        return compact;
    }

    public static boolean sameMac(String left, String right) {
        String compactLeft = toCompactForm(left);
        String compactRight = toCompactForm(right);
        if (compactLeft == null || compactRight == null) {
            return false;
        }
        return compactLeft.equalsIgnoreCase(compactRight);
    }
}
