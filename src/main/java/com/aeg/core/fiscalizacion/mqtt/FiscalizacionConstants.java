package com.aeg.core.fiscalizacion.mqtt;

public final class FiscalizacionConstants {

    public static final String CMD_PTR_FISCALIZAR = "ptrFiscalizar";
    public static final String CMD_PTR_FISCALIZAR_REMOTO = "ptrFiscalizarRemoto";
    public static final String CMD_RX_PTR_FISCALIZAR_REMOTO = "RxPtrFiscalizarRemoto";

    public static final String MSG_REGISTRO_EXISTE = "Registro de Impresora ya Existe";
    public static final String MSG_MAC_EXISTE = "Mac Address de Impresora ya Existe";
    public static final String MSG_PRECINTO_NO_EXISTE = "Precinto de Impresora no Existe";
    public static final String MSG_PRECINTO_ASIGNADO = "Precinto de Impresora ya está Asignado";
    public static final String MSG_MODELO_NO_EXISTE = "Modelo de Impresora no Existe";
    public static final String MSG_LISTA = "Impresora Lista a Fiscalizar";

    public static final String STEP_REQUEST = "request";
    public static final String STEP_ACK = "ack";
    public static final String STEP_RESULT = "result";

    private FiscalizacionConstants() {
    }
}
