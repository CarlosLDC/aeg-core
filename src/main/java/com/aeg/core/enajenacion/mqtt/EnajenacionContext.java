package com.aeg.core.enajenacion.mqtt;

import java.util.List;

public record EnajenacionContext(
        String fiscalSerial,
        String macAddress,
        Long clientId,
        String rif,
        String businessName,
        String contributorTypeLine,
        String addressLine1,
        String addressLine2,
        String cityStateLine,
        List<String> encFacFijoLines,
        List<String> pieFacFijoLines) {
}
