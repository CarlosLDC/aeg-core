package com.aeg.core.firmware.dto;

import com.aeg.core.firmware.FirmwareUploadJobStatus;

public record FirmwareUploadJobResponse(
		String jobId,
		FirmwareUploadJobStatus status,
		String error,
		FirmwareResponse result) {
}
