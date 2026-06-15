package com.aeg.core.enajenacion.mqtt;

public final class EnajenacionConstants {

    public static final String CMD_PTR_ENAJENAR = "ptrEnajenar";
    public static final String CMD_END_DNF = "endDNF";
    public static final String CMD_FISCAL_AEG = "fiscalAEG";
    public static final String CMD_W_FILE_SPIFF = "wFileSPIFF";
    public static final String CMD_SUB_TO_F = "subToF";
    public static final String CMD_END_FAC = "endFac";
    public static final String CMD_PROD_NC = "prodNC";
    public static final String CMD_END_PO_NC = "endPoNC";
    public static final String CMD_END_NC = "endNC";
    public static final String CMD_GEN_IMP_REP_Z = "genImpRepZ";
    public static final String CMD_STA_INF = "StaInf";

    public static final String STA_INF_STATUS_NRO_REG_MA = "NroRegMa";

    public static final int DNF_END_OK = 7;
    public static final int INVOICE_END_OK = 8;
    public static final int CREDIT_NOTE_END_OK = 10;
    public static final int SUBTOTAL_DATA_D = 555;
    public static final int PROD_NC_LINE_DATA_D = 9;

    private EnajenacionConstants() {
    }
}
