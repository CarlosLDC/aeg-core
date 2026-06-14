package com.aeg.core.enajenacion.mqtt;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RifFormatter {

    private static final Pattern RIF_WITHOUT_HYPHEN = Pattern.compile("^[VEJPG][0-9]{7,9}$", Pattern.CASE_INSENSITIVE);

    private RifFormatter() {
    }

    public static String toFiscalForm(String rif) {
        if (rif == null || rif.isBlank()) {
            return rif;
        }
        String trimmed = rif.trim().toUpperCase(Locale.ROOT);
        if (trimmed.contains("-")) {
            return trimmed;
        }
        if (RIF_WITHOUT_HYPHEN.matcher(trimmed).matches()) {
            return trimmed.charAt(0) + "-" + trimmed.substring(1);
        }
        return trimmed;
    }
}
