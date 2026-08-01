package com.aeg.core.firmware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FirmwareUpdateRequest(
		@NotBlank
		@Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+$", message = "version must match N.N.N (e.g. 1.2.3)")
		String version,
		Long printerModelId,
		String notes) {
}
