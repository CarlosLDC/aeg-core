package com.aeg.core.fiscalbook.dto;

import jakarta.validation.constraints.NotBlank;

public record FiscalBookLookupInspectionByQrRequest(
		@NotBlank String qrCodigo) {
}
