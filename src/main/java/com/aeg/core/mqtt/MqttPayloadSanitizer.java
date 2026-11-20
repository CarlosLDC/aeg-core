package com.aeg.core.mqtt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MqttPayloadSanitizer {

    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"(?:pass|password|passwBroker|userBroker|Access|llaveEncrip|encryptionKey|token|secret)\"\\s*:\\s*)\"((?:[^\"\\\\]|\\\\.)*)\"",
            Pattern.CASE_INSENSITIVE
    );

    private MqttPayloadSanitizer() {
    }

    public static String sanitize(String payload) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }
        Matcher matcher = SENSITIVE_JSON_FIELD.matcher(payload);
        if (!matcher.find()) {
            return payload;
        }
        return matcher.replaceAll("$1\"***\"");
    }
}
