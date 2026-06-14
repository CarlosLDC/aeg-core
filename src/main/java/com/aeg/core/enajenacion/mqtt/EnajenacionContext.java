package com.aeg.core.enajenacion.mqtt;

public record EnajenacionContext(
        String fiscalSerial,
        String macAddress,
        Long clientId,
        String rif,
        String businessName,
        String contributorTypeLine,
        String addressLine1,
        String addressLine2,
        String cityStateLine) {
}
