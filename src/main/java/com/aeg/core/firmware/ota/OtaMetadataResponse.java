package com.aeg.core.firmware.ota;

/**
 * JSON payload returned to the ESP32 when it queries for the latest firmware.
 *
 * <ul>
 *   <li>{@code modelPtr}  – model code used to look up the firmware.</li>
 *   <li>{@code hardVer}   – hardware version (not yet tracked in the backend; always {@code null}).</li>
 *   <li>{@code firmVer}   – semantic version string of the firmware (e.g. {@code "1.1.0"}).</li>
 *   <li>{@code buildDate} – Unix timestamp (seconds) derived from {@code Firmware.createdAt}.</li>
 *   <li>{@code checksum}  – MD5 hex digest of the binary; the ESP32 validates this after download.</li>
 *   <li>{@code fileSize}  – binary size in bytes.</li>
 * </ul>
 */
public record OtaMetadataResponse(
        String modelPtr,
        String hardVer,
        String firmVer,
        long buildDate,
        String checksum,
        long fileSize) {
}
