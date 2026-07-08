package com.aeg.core.tools.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ToolsMqttSettings {

    @Value("${app.mqtt.tools.timeout.status-seconds:15}")
    private int statusTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.wifi-seconds:30}")
    private int wifiTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.wifi-scan-seconds:10}")
    private int wifiScanTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.report-z-seconds:20}")
    private int reportZTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.formas-pago-seconds:10}")
    private int formasPagoTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.reprint-seconds:60}")
    private int reprintTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.default-seconds:15}")
    private int defaultTimeoutSeconds;

    public int statusTimeoutSeconds() {
        return statusTimeoutSeconds;
    }

    public int wifiTimeoutSeconds() {
        return wifiTimeoutSeconds;
    }

    public int wifiScanTimeoutSeconds() {
        return wifiScanTimeoutSeconds;
    }

    public int reportZTimeoutSeconds() {
        return reportZTimeoutSeconds;
    }

    public int formasPagoTimeoutSeconds() {
        return formasPagoTimeoutSeconds;
    }

    public int reprintTimeoutSeconds() {
        return reprintTimeoutSeconds;
    }

    public int defaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    @Value("${app.mqtt.tools.timeout.test-invoice-seconds:5}")
    private int testInvoiceTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.test-note-seconds:6}")
    private int testNoteTimeoutSeconds;

    @Value("${app.mqtt.tools.timeout.test-generate-z-seconds:5}")
    private int testGenerateZTimeoutSeconds;

    public int testInvoiceTimeoutSeconds() {
        return testInvoiceTimeoutSeconds;
    }

    public int testNoteTimeoutSeconds() {
        return testNoteTimeoutSeconds;
    }

    public int testGenerateZTimeoutSeconds() {
        return testGenerateZTimeoutSeconds;
    }
}
