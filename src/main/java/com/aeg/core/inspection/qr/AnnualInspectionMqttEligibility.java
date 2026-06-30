package com.aeg.core.inspection.qr;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterStatus;

public final class AnnualInspectionMqttEligibility {

    private AnnualInspectionMqttEligibility() {
    }

    public static boolean isEligible(Printer printer) {
        if (printer == null) {
            return false;
        }
        return printer.getStatus() == PrinterStatus.ENAJENADA
                && printer.getClientId() != null
                && printer.getFiscalSerial() != null
                && !printer.getFiscalSerial().isBlank()
                && printer.getMacAddress() != null
                && !printer.getMacAddress().isBlank();
    }
}
