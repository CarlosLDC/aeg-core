package com.aeg.core.fiscalbook.dto;

public record FiscalBookLookupInspectionByQrResponse(
		Long inspectionId,
		Long printerId,
		String fiscalSerial,
		String registro,
		String mac,
		String fecha) {
}
