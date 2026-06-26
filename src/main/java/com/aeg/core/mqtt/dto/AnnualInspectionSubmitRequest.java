package com.aeg.core.mqtt.dto;

import jakarta.validation.constraints.NotNull;

public record AnnualInspectionSubmitRequest(
        @NotNull Long printerId,
        boolean chkPrecinto,
        boolean chkEtiquetaFiscal,
        boolean chkFactura,
        boolean chkNotaCredito,
        boolean chkSensorPapel) {}
