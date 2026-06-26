package com.aeg.core.printer;

import java.util.ArrayList;
import java.util.List;

public final class PrinterTicketValidator {

    public static final int MAX_LINE_LENGTH = 68;

    private PrinterTicketValidator() {
    }

    public static PrinterTicketSection normalizeHeader(PrinterTicketSection header) {
        if (header == null || header.lines() == null || header.lines().isEmpty()) {
            throw new IllegalArgumentException("header.lines is required and must not be empty");
        }
        List<String> lines = normalizeLines(header.lines(), true);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("header.lines is required and must not be empty");
        }
        return new PrinterTicketSection(List.copyOf(lines));
    }

    public static PrinterTicketSection normalizeTrailer(PrinterTicketSection trailer) {
        if (trailer == null || trailer.lines() == null) {
            return new PrinterTicketSection(List.of());
        }
        return new PrinterTicketSection(List.copyOf(normalizeLines(trailer.lines(), false)));
    }

    private static List<String> normalizeLines(List<String> rawLines, boolean required) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            if (rawLine == null) {
                if (required) {
                    throw new IllegalArgumentException("header lines must not contain null values");
                }
                continue;
            }
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) {
                if (required) {
                    throw new IllegalArgumentException("header lines must not be blank");
                }
                continue;
            }
            if (trimmed.length() > MAX_LINE_LENGTH) {
                throw new IllegalArgumentException(
                        "Ticket line exceeds maximum length of " + MAX_LINE_LENGTH + " characters");
            }
            lines.add(trimmed);
        }
        return lines;
    }
}
