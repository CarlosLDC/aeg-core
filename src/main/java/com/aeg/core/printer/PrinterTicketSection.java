package com.aeg.core.printer;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record PrinterTicketSection(@NotNull List<String> lines) {
}
