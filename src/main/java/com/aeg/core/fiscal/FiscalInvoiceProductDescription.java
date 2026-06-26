package com.aeg.core.fiscal;

import java.util.ArrayList;
import java.util.List;

/**
 * Límites de descripción de producto en facturas fiscales venezolanas.
 * Una sola línea: 39 caracteres (el resto del ancho lo ocupa el precio).
 * Varias líneas: 60 caracteres por línea, hasta 5 líneas en proF.
 */
public final class FiscalInvoiceProductDescription {

    public static final int SINGLE_LINE_MAX_LENGTH = 39;
    public static final int MULTI_LINE_MAX_LENGTH = 60;
    public static final int MAX_LINES = 5;

    private FiscalInvoiceProductDescription() {
    }

    public static List<String> resolveLines(String productDescription, String defaultDescription) {
        String raw = productDescription == null || productDescription.isBlank()
                ? defaultDescription
                : productDescription.trim();

        List<String> sourceLines = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                sourceLines.add(trimmed);
            }
        }

        if (sourceLines.isEmpty()) {
            return List.of(truncateLine(
                    FiscalTicketLatin2.normalizeFiscalTicketText(defaultDescription),
                    SINGLE_LINE_MAX_LENGTH));
        }

        if (sourceLines.size() == 1) {
            return List.of(truncateLine(
                    FiscalTicketLatin2.normalizeFiscalTicketText(sourceLines.get(0)),
                    SINGLE_LINE_MAX_LENGTH));
        }

        List<String> lines = new ArrayList<>();
        int limit = Math.min(sourceLines.size(), MAX_LINES);
        for (int index = 0; index < limit; index++) {
            lines.add(truncateLine(
                    FiscalTicketLatin2.normalizeFiscalTicketText(sourceLines.get(index)),
                    MULTI_LINE_MAX_LENGTH));
        }
        return List.copyOf(lines);
    }

    public static List<String> linesForProfCommands(List<String> descriptionLines) {
        if (descriptionLines.isEmpty()) {
            return List.of("", "", "", "", "");
        }
        boolean singleLine = descriptionLines.size() == 1;
        List<String> result = new ArrayList<>(MAX_LINES);
        for (int imp = 1; imp <= MAX_LINES; imp++) {
            if (singleLine) {
                result.add(descriptionLines.get(0));
            } else {
                result.add(imp <= descriptionLines.size() ? descriptionLines.get(imp - 1) : "");
            }
        }
        return List.copyOf(result);
    }

    private static String truncateLine(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
