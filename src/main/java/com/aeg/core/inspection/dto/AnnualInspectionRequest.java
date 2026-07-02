package com.aeg.core.inspection.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record AnnualInspectionRequest(
		@NotNull Long printerId,
		@NotNull Long userId,
		@NotNull Boolean sealTampered,
		String notes,
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
