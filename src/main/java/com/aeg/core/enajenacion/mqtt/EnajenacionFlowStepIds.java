package com.aeg.core.enajenacion.mqtt;

import java.util.Optional;

/**
 * Step identifiers aligned with the admin panel ({@code ENAJENACION_FLOW_STEPS}).
 */
public final class EnajenacionFlowStepIds {

    public static final String REQUEST = "request";
    public static final String DNF = "dnf";
    public static final String FISCAL_RIF = "fiscal-rif";
    public static final String HEADER = "header";
    public static final String CONFIG = "config";
    public static final String REG_STATUS = "reg-status";
    public static final String INVOICE = "invoice";
    public static final String CREDIT_NOTE = "credit-note";
    public static final String REPORT_Z = "report-z";

    private EnajenacionFlowStepIds() {
    }

    /**
     * Flow step whose printer response was just accepted while in the given {@code *_SENT} state.
     */
    public static Optional<String> acceptedStepId(EnajenacionSessionState awaitingSentState) {
        return switch (awaitingSentState) {
            case DNF_SENT -> Optional.of(DNF);
            case FISCAL_RIF_SENT -> Optional.of(FISCAL_RIF);
            case HEADER_SENT -> Optional.of(HEADER);
            case CONFIG_SENT -> Optional.of(CONFIG);
            case REG_STATUS_SENT -> Optional.of(REG_STATUS);
            case INVOICE_SENT -> Optional.of(INVOICE);
            case CREDIT_NOTE_SENT -> Optional.of(CREDIT_NOTE);
            case REPORT_Z_SENT -> Optional.of(REPORT_Z);
            default -> Optional.empty();
        };
    }

    /**
     * Flow step for the command published when entering the given {@code *_SENT} state.
     */
    public static Optional<String> publishedStepId(EnajenacionSessionState sentState) {
        return switch (sentState) {
            case DNF_SENT -> Optional.of(DNF);
            case FISCAL_RIF_SENT -> Optional.of(FISCAL_RIF);
            case HEADER_SENT -> Optional.of(HEADER);
            case CONFIG_SENT -> Optional.of(CONFIG);
            case REG_STATUS_SENT -> Optional.of(REG_STATUS);
            case INVOICE_SENT -> Optional.of(INVOICE);
            case CREDIT_NOTE_SENT -> Optional.of(CREDIT_NOTE);
            case REPORT_Z_SENT -> Optional.of(REPORT_Z);
            default -> Optional.empty();
        };
    }
}
