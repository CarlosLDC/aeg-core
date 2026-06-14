package com.aeg.core.enajenacion.mqtt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.ScheduledFuture;

public final class EnajenacionSession {

    private final String compactMac;
    private final Long printerId;
    private final EnajenacionContext context;
    private final Instant startedAt;

    private volatile EnajenacionSessionState state = EnajenacionSessionState.VALIDATED;
    private volatile EnajenacionAwaitingKind awaitingKind;
    private volatile ScheduledFuture<?> timeoutTask;
    private volatile String lastError;
    private volatile int invoiceNumber = 1;
    private volatile LocalDate invoiceDate = LocalDate.now();

    public EnajenacionSession(String compactMac, Long printerId, EnajenacionContext context) {
        this.compactMac = compactMac;
        this.printerId = printerId;
        this.context = context;
        this.startedAt = Instant.now();
    }

    public String compactMac() {
        return compactMac;
    }

    public Long printerId() {
        return printerId;
    }

    public EnajenacionContext context() {
        return context;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public EnajenacionSessionState state() {
        return state;
    }

    public void setState(EnajenacionSessionState state) {
        this.state = state;
    }

    public EnajenacionAwaitingKind awaitingKind() {
        return awaitingKind;
    }

    public void setAwaiting(EnajenacionAwaitingKind kind) {
        this.awaitingKind = kind;
    }

    public void clearAwaiting() {
        this.awaitingKind = null;
    }

    public ScheduledFuture<?> timeoutTask() {
        return timeoutTask;
    }

    public void setTimeoutTask(ScheduledFuture<?> timeoutTask) {
        this.timeoutTask = timeoutTask;
    }

    public String lastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public int invoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate invoiceDate() {
        return invoiceDate;
    }

    public boolean isTerminal() {
        return state == EnajenacionSessionState.COMPLETED || state == EnajenacionSessionState.FAILED;
    }

    public boolean isAwaitingResponse() {
        return awaitingKind != null && !isTerminal();
    }
}
