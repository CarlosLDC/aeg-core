package com.aeg.core.enajenacion.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnajenacionMqttSettings {

    @Value("${app.mqtt.enajenacion.enabled:true}")
    private boolean enabled = true;

    @Value("${app.mqtt.enajenacion.inbound-topic:+/AEG_Fiscal/Integracion/CmdServer}")
    private String inboundTopic = "+/AEG_Fiscal/Integracion/CmdServer";

    @Value("${app.mqtt.enajenacion.skip-registration-status:true}")
    private boolean skipRegistrationStatus = true;

    @Value("${app.mqtt.enajenacion.timeout.dnf-seconds:180}")
    private int dnfTimeoutSeconds;

    @Value("${app.mqtt.enajenacion.timeout.fiscal-rif-seconds:120}")
    private int fiscalRifTimeoutSeconds;

    @Value("${app.mqtt.enajenacion.timeout.config-seconds:60}")
    private int configTimeoutSeconds;

    @Value("${app.mqtt.enajenacion.timeout.invoice-seconds:180}")
    private int invoiceTimeoutSeconds;

    @Value("${app.mqtt.enajenacion.timeout.credit-note-seconds:180}")
    private int creditNoteTimeoutSeconds;

    @Value("${app.mqtt.enajenacion.timeout.report-z-seconds:120}")
    private int reportZTimeoutSeconds;

    public boolean enabled() {
        return enabled;
    }

    public String inboundTopic() {
        return inboundTopic;
    }

    public boolean skipRegistrationStatus() {
        return skipRegistrationStatus;
    }

    public int dnfTimeoutSeconds() {
        return dnfTimeoutSeconds;
    }

    public int fiscalRifTimeoutSeconds() {
        return fiscalRifTimeoutSeconds;
    }

    public int configTimeoutSeconds() {
        return configTimeoutSeconds;
    }

    public int invoiceTimeoutSeconds() {
        return invoiceTimeoutSeconds;
    }

    public int creditNoteTimeoutSeconds() {
        return creditNoteTimeoutSeconds;
    }

    public int reportZTimeoutSeconds() {
        return reportZTimeoutSeconds;
    }
}
