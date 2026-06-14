package com.aeg.core.enajenacion.mqtt;

public enum EnajenacionSessionState {
    VALIDATED,
    DNF_SENT,
    DNF_OK,
    FISCAL_RIF_SENT,
    FISCAL_RIF_OK,
    HEADER_SENT,
    HEADER_OK,
    CONFIG_SENT,
    CONFIG_OK,
    REG_STATUS_SENT,
    REG_STATUS_OK,
    INVOICE_SENT,
    INVOICE_OK,
    CREDIT_NOTE_SENT,
    CREDIT_NOTE_OK,
    REPORT_Z_SENT,
    COMPLETED,
    FAILED
}
