package com.aeg.core.inspection.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AnnualInspectionResponse(
		Long id,
		Long printerId,
		Long userId,
		Boolean sealTampered,
		String notes,
		OffsetDateTime createdAt,
		LocalDate inspectionDate,
		String mqttRegistroImpresora,
		Long mqttSetDateRevOAt,
		Integer mqttNumeroFacturaPrueba,
		Boolean chkPrecinto,
		Boolean chkEtiquetaFiscal,
		Boolean chkFactura,
		Boolean chkNotaCredito,
		Boolean chkSensorPapel,
		String mqttQrCodigo,
		String mqttQrRegistro,
		String mqttQrMac,
		String mqttQrFecha) {
}
