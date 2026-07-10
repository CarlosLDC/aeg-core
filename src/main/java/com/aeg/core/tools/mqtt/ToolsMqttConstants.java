package com.aeg.core.tools.mqtt;

public final class ToolsMqttConstants {

    public static final String CMD_STA_INF = "StaInf";
    public static final String CMD_WIFI_CONF = "wifiConf";
    public static final String CMD_RESET_MF = "resetMF";
    public static final String CMD_GET_REP_Z = "getRepZ";
    public static final String CMD_REP_Z = "RepZ";
    public static final String CMD_IMP_REP_X = "impRepX";
    public static final String CMD_DESC_FP = "descFP";
    public static final String CMD_W_FILE_SPIFF = "wFileSPIFF";
    public static final String CMD_PIE_TI_F = "pieTiF";
    public static final String CMD_REIM_REP = "reimRep";

    public static final String CMD_END_FAC = "endFac";
    public static final String CMD_END_NC = "endNC";
    public static final String CMD_END_ND = "endND";
    public static final String CMD_GEN_IMP_REP_Z = "genImpRepZ";

    public static final String TEST_PRODUCT_DESCRIPTION = "PRODUCTO";
    public static final String TEST_NOTE_DATE = "02/06/2025";

    public static final String STA_CONEXION_SIN_DNF = "StaConexionSinDNF";
    public static final String STA_GET_ACC_POI = "GetAccPoi";
    public static final String STA_ULT_Z_TX_SENI = "UltZTxSeni";
    public static final String STA_MEDIOS_PAGOS = "MediosPagos";
    public static final String STA_ENC_FIJ = "staEncFij";
    public static final String STA_PIE_FIJ = "staPieFij";

    public static final String SPIFF_ACCESS = "AeG-1968-2024";
    public static final String PARAM_FAC_SPIFF_FILE = "paramFacSPIFF.json";

    public static final int HEADER_MAX_LINES = 8;
    public static final int FOOTER_MAX_LINES = 9;
    public static final int HEADER_FOOTER_MAX_LINE_LENGTH = 50;

    public static final int RESET_MF_DATA = 5555;

    private ToolsMqttConstants() {
    }
}
