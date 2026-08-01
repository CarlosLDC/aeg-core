package com.aeg.core.fiscalizacion.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FiscalizacionMqttSettings {

    @Value("${app.mqtt.fiscalizacion.enabled:true}")
    private boolean enabled = true;

    @Value("${app.mqtt.fiscalizacion.timeout.result-seconds:180}")
    private int resultTimeoutSeconds = 180;

    public boolean enabled() {
        return enabled;
    }

    public int resultTimeoutSeconds() {
        return resultTimeoutSeconds;
    }
}
