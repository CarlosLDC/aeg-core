package com.aeg.core.enajenacion.mqtt;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RifFormatter {

    public static final String FISCAL_RIF_PATTERN = "^(?i)[VEJPG]-?[0-9]{8}-?[0-9]$";
    public static final String FISCAL_RIF_ERROR_MESSAGE =
            "El RIF debe tener formato V, E, J, P o G seguido de 9 dígitos (ej. J-123456789). Si tiene menos dígitos, completa con ceros a la izquierda.";

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
        String clean = rif.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (RIF_WITHOUT_HYPHEN.matcher(clean).matches()) {
            return clean.charAt(0) + "-" + clean.substring(1);
        }
        return rif.trim().toUpperCase(Locale.ROOT);
    }
}
