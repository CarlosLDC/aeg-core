package com.aeg.core.config;

final class MqttClientIds {

    private MqttClientIds() {
    }

    /**
     * Sufijo por instancia para evitar colisiones de client-id en el broker (varias réplicas / redespliegues).
     */
    static String suffix() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname.trim();
        }
        String instance = System.getenv("DO_APP_PLATFORM_INSTANCE_ID");
        if (instance != null && !instance.isBlank()) {
            return instance.trim();
        }
        return "local";
    }

    static String clientId(String base) {
        return base + "-" + suffix();
    }
}
