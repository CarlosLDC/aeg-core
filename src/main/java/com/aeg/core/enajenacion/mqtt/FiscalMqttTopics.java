package com.aeg.core.enajenacion.mqtt;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FiscalMqttTopics {

    /**
     * Tópicos fiscales AEG (misma convención en enajenación e inspección anual):
     * <ul>
     *   <li>{@link #CMD_SERVER_SUFFIX} — impresora → servidor; solo {@code ptrEnajenar} al arrancar (enajenación).</li>
     *   <li>{@link #COMANDO_SUFFIX} — servidor → impresora; todos los comandos (pasos 2–7 y flujo de inspección anual).</li>
     *   <li>{@link #RESPUESTA_SUFFIX} — impresora → servidor; respuestas del firmware ({@code code}, {@code dataD}, {@code dataS}).</li>
     *   <li>{@link #DOCUMENTO_SUFFIX} — impresora → servidor; fragmentos ESC/POS de documentos (visualización).</li>
     * </ul>
     * Formato: {@code /{mac12hex}/AEG_Fiscal/Integracion/{CmdServer|Comando|Respuesta|Documento}}.
     */
    public static final String CMD_SERVER_SUFFIX = "/AEG_Fiscal/Integracion/CmdServer";
    public static final String COMANDO_SUFFIX = "/AEG_Fiscal/Integracion/Comando";
    public static final String RESPUESTA_SUFFIX = "/AEG_Fiscal/Integracion/Respuesta";
    public static final String DOCUMENTO_SUFFIX = "/AEG_Fiscal/Integracion/Documento";

    private static final Pattern INBOUND_TOPIC = Pattern.compile(
            "^/?([0-9A-Fa-f]{12})/AEG_Fiscal/Integracion/(CmdServer|Respuesta|Documento)$");

    private FiscalMqttTopics() {
    }

    public static String comandoTopic(String compactMac) {
        return "/" + MacAddressNormalizer.toCompactForm(compactMac) + COMANDO_SUFFIX;
    }

    public static String respuestaTopic(String compactMac) {
        return "/" + MacAddressNormalizer.toCompactForm(compactMac) + RESPUESTA_SUFFIX;
    }

    public static boolean isCmdServerTopic(String topic) {
        return topic != null && topic.trim().endsWith(CMD_SERVER_SUFFIX);
    }

    public static boolean isRespuestaTopic(String topic) {
        return topic != null && topic.trim().endsWith(RESPUESTA_SUFFIX);
    }

    public static boolean isDocumentoTopic(String topic) {
        return topic != null && topic.trim().endsWith(DOCUMENTO_SUFFIX);
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
