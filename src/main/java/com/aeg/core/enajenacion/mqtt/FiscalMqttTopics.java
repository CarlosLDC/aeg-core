package com.aeg.core.enajenacion.mqtt;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FiscalMqttTopics {

    public static final String CMD_SERVER_SUFFIX = "/AEG_Fiscal/Integracion/CmdServer";
    public static final String COMANDO_SUFFIX = "/AEG_Fiscal/Integracion/Comando";

    private static final Pattern INBOUND_TOPIC = Pattern.compile(
            "^/?([0-9A-Fa-f]{12})/AEG_Fiscal/Integracion/(CmdServer|Respuesta)$");

    private FiscalMqttTopics() {
    }

    public static String comandoTopic(String compactMac) {
        return MacAddressNormalizer.toCompactForm(compactMac) + COMANDO_SUFFIX;
    }

    public static Optional<String> extractCompactMac(String topic) {
        if (topic == null || topic.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = INBOUND_TOPIC.matcher(topic.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1).toUpperCase());
    }

    public static boolean isFiscalInboundTopic(String topic) {
        return extractCompactMac(topic).isPresent();
    }
}
