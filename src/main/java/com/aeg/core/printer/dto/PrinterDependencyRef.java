package com.aeg.core.printer.dto;

public record PrinterDependencyRef(
		String type,
		Long id,
		String label) {
}
