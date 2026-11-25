package com.aeg.core.enajenacion.mqtt;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RifFormatter {

    public static final String FISCAL_RIF_PATTERN = "^[VEJPG]-?[0-9]{9}$";
    public static final String FISCAL_RIF_ERROR_MESSAGE =
            "El RIF debe tener formato V, E, J, P o G seguido de 9 dígitos (ej. J-123456789).";

    private static final Pattern RIF_WITHOUT_HYPHEN = Pattern.compile("^[VEJPG][0-9]{7,9}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRICT_FISCAL_RIF = Pattern.compile("^[VEJPG]-[0-9]{9}$");

    private RifFormatter() {
    }

    public static boolean isValidFiscalRif(String rif) {
        if (rif == null || rif.isBlank()) {
            return false;
        }
        String formatted = toFiscalForm(rif);
        return formatted != null && STRICT_FISCAL_RIF.matcher(formatted).matches();
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
