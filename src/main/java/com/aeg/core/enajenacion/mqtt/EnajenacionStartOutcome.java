package com.aeg.core.enajenacion.mqtt;

public record EnajenacionStartOutcome(EnajenacionStartStatus status, String message) {

    public static EnajenacionStartOutcome started() {
        return new EnajenacionStartOutcome(EnajenacionStartStatus.STARTED, null);
    }

    public static EnajenacionStartOutcome rejected(String message) {
        return new EnajenacionStartOutcome(EnajenacionStartStatus.REJECTED, message);
    }

    public static EnajenacionStartOutcome alreadyCompleted() {
        return new EnajenacionStartOutcome(
                EnajenacionStartStatus.ALREADY_COMPLETED,
                "Printer already enajenada");
    }

    public static EnajenacionStartOutcome skipped() {
        return new EnajenacionStartOutcome(EnajenacionStartStatus.SKIPPED, null);
    }
}
