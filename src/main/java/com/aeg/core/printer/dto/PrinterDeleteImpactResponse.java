package com.aeg.core.printer.dto;

import java.util.List;

public record PrinterDeleteImpactResponse(
		Long printerId,
		String fiscalSerial,
		List<PrinterDependencyRef> dependencies,
		List<String> consequences,
		boolean requiresForce) {
}
