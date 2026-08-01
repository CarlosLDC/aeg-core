package com.aeg.core.fiscalizacion.mqtt;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

public final class FiscalizacionSession {

    private final String compactMac;
    private final FiscalizacionContext context;
    private final Instant startedAt;

    private volatile FiscalizacionSessionState state = FiscalizacionSessionState.ACK_SENT;
    private volatile boolean awaitingResponse;
    private volatile Instant awaitingSince;
    private volatile Integer awaitingTimeoutSeconds;
    private volatile ScheduledFuture<?> timeoutTask;
    private volatile String lastError;
    private volatile Long printerId;

    public FiscalizacionSession(String compactMac, FiscalizacionContext context) {
        this.compactMac = compactMac;
        this.context = context;
        this.startedAt = Instant.now();
    }

    public String compactMac() {
        return compactMac;
    }

    public FiscalizacionContext context() {
        return context;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public FiscalizacionSessionState state() {
        return state;
    }

    public void setState(FiscalizacionSessionState state) {
        this.state = state;
    }

    public boolean isAwaitingResponse() {
        return awaitingResponse && !isTerminal();
    }

    public void setAwaitingResponse(boolean awaitingResponse) {
        this.awaitingResponse = awaitingResponse;
    }

    public Instant awaitingSince() {
        return awaitingSince;
    }

    public void setAwaitingSince(Instant awaitingSince) {
        this.awaitingSince = awaitingSince;
    }

    public Integer awaitingTimeoutSeconds() {
        return awaitingTimeoutSeconds;
    }

    public void setAwaitingTimeoutSeconds(Integer awaitingTimeoutSeconds) {
        this.awaitingTimeoutSeconds = awaitingTimeoutSeconds;
    }

    public void clearAwaiting() {
        this.awaitingResponse = false;
        this.awaitingSince = null;
        this.awaitingTimeoutSeconds = null;
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

    public Long printerId() {
        return printerId;
    }

    public void setPrinterId(Long printerId) {
        this.printerId = printerId;
    }

    public boolean isTerminal() {
        return state == FiscalizacionSessionState.COMPLETED || state == FiscalizacionSessionState.FAILED;
    }
}
