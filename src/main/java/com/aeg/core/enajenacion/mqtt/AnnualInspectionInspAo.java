package com.aeg.core.enajenacion.mqtt;

public record AnnualInspectionInspAo(
        String precinto,
        String etiqFisc,
        String impFact,
        String impNC,
        String sensPapel) {

    public static final String STATUS_OK = "Bien";
    public static final String STATUS_VIOLATED = "Violentado";
    public static final String STATUS_DEFECTIVE = "Defectuoso";

    public static AnnualInspectionInspAo fromChecklist(
            boolean chkPrecinto,
            boolean chkEtiquetaFiscal,
            boolean chkFactura,
            boolean chkNotaCredito,
            boolean chkSensorPapel) {
        return new AnnualInspectionInspAo(
                chkPrecinto ? STATUS_OK : STATUS_VIOLATED,
                chkEtiquetaFiscal ? STATUS_OK : STATUS_VIOLATED,
                chkFactura ? STATUS_OK : STATUS_DEFECTIVE,
                chkNotaCredito ? STATUS_OK : STATUS_DEFECTIVE,
                chkSensorPapel ? STATUS_OK : STATUS_DEFECTIVE);
    }
}
