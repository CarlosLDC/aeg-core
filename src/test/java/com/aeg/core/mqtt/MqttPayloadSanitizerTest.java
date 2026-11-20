package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MqttPayloadSanitizerTest {

    @Test
    void redactsSensitiveJsonCredentialsWhilePreservingOtherFields() {
        String raw = "{\"cmd\":\"configMqttAEG\",\"userBroker\":\"aeguser\",\"passwBroker\":\"secret123\","
                + "\"Access\":\"AeG-Secret\",\"llaveEncrip\":\"55a42534f4d2d8b9\",\"pass\":\"wifiPass!\",\"port\":1883}";

        String sanitized = MqttPayloadSanitizer.sanitize(raw);

        assertThat(sanitized).contains("\"cmd\":\"configMqttAEG\"");
        assertThat(sanitized).contains("\"userBroker\":\"***\"");
        assertThat(sanitized).contains("\"passwBroker\":\"***\"");
        assertThat(sanitized).contains("\"Access\":\"***\"");
        assertThat(sanitized).contains("\"llaveEncrip\":\"***\"");
        assertThat(sanitized).contains("\"pass\":\"***\"");
        assertThat(sanitized).doesNotContain("aeguser", "secret123", "AeG-Secret", "55a42534f4d2d8b9", "wifiPass!");
    }

    @Test
    void passesThroughNonSensitiveOrNullPayloadsUnchanged() {
        assertThat(MqttPayloadSanitizer.sanitize(null)).isNull();
        assertThat(MqttPayloadSanitizer.sanitize("")).isEmpty();
        String plain = "{\"cmd\":\"StaInf\",\"code\":0}";
        assertThat(MqttPayloadSanitizer.sanitize(plain)).isEqualTo(plain);
    }
}
