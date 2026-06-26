package com.aeg.core.fiscal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Texto de tickets y payloads MQTT para impresoras fiscales venezolanas (ISO/IEC 8859-2).
 */
public final class FiscalTicketLatin2 {

    public static final Charset CHARSET = Charset.forName("ISO-8859-2");

    private static final int[] ISO_8859_2_CODEPOINTS = {
            0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087, 0x0088,
            0x0089, 0x008a, 0x008b, 0x008c, 0x008d, 0x008e, 0x008f, 0x0090, 0x0091,
            0x0092, 0x0093, 0x0094, 0x0095, 0x0096, 0x0097, 0x0098, 0x0099, 0x009a,
            0x009b, 0x009c, 0x009d, 0x009e, 0x009f, 0x00a0, 0x0104, 0x02d8, 0x0141,
            0x00a4, 0x013d, 0x015a, 0x00a7, 0x00a8, 0x0160, 0x015e, 0x0164, 0x0179,
            0x00ad, 0x017d, 0x017b, 0x00b0, 0x0105, 0x02db, 0x0142, 0x00b4, 0x013e,
            0x015b, 0x02c7, 0x00b8, 0x0161, 0x015f, 0x0165, 0x017a, 0x02dd, 0x017e,
            0x017c, 0x0154, 0x00c1, 0x00c2, 0x0102, 0x00c4, 0x0139, 0x0106, 0x00c7,
            0x010c, 0x00c9, 0x0118, 0x00cb, 0x011a, 0x00cd, 0x00ce, 0x010e, 0x0110,
            0x0143, 0x0147, 0x00d3, 0x00d4, 0x0150, 0x00d6, 0x00d7, 0x0158, 0x016e,
            0x00da, 0x0170, 0x00dc, 0x00dd, 0x0162, 0x00df, 0x0155, 0x00e1, 0x00e2,
            0x0103, 0x00e4, 0x013a, 0x0107, 0x00e7, 0x010d, 0x00e9, 0x0119, 0x00eb,
            0x011b, 0x00ed, 0x00ee, 0x010f, 0x0111, 0x0144, 0x0148, 0x00f3, 0x00f4,
            0x0151, 0x00f6, 0x00f7, 0x0159, 0x016f, 0x00fa, 0x0171, 0x00fc, 0x00fd,
            0x0163, 0x02d9,
    };

    private static final boolean[] ENCODABLE = new boolean[0x110000];

    static {
        for (int index = 0; index < ISO_8859_2_CODEPOINTS.length; index++) {
            ENCODABLE[ISO_8859_2_CODEPOINTS[index]] = true;
        }
    }

    private FiscalTicketLatin2() {
    }

    public static String normalizeFiscalTicketText(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? null : "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        normalized = normalized
                .replace("\u2014", "-")
                .replace("\u2013", "-")
                .replace("\u2212", "-")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201c", "\"")
                .replace("\u201d", "\"")
                .replace("\u2026", "...")
                .replace("\u202f", " ")
                .replace('\u00a0', ' ');

        StringBuilder out = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (codePoint < 0x80) {
                out.appendCodePoint(codePoint);
                return;
            }
            if (isEncodable(codePoint)) {
                out.appendCodePoint(codePoint);
                return;
            }
            String decomposed = Normalizer.normalize(new String(Character.toChars(codePoint)), Normalizer.Form.NFD);
            if (decomposed.chars().allMatch(ch -> ch < 0x80)) {
                out.append(decomposed);
                return;
            }
            out.append('?');
        });
        return out.toString();
    }

    public static List<String> normalizeFiscalTicketLines(List<String> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream().map(FiscalTicketLatin2::normalizeFiscalTicketText).toList();
    }

    @SuppressWarnings("unchecked")
    public static Object normalizePayloadValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return normalizeFiscalTicketText(text);
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object entry : list) {
                normalized.add(normalizePayloadValue(entry));
            }
            return normalized;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalizePayloadValue(entry.getValue()));
            }
            return normalized;
        }
        return value;
    }

    public static byte[] encodePayload(String payload) {
        return payload.getBytes(CHARSET);
    }

    public static boolean isFiscalPrinterTopic(String topic) {
        return topic != null && topic.contains("AEG_Fiscal/Integracion");
    }

    public static byte[] encodeMqttPayload(String topic, String payload) {
        Charset charset = isFiscalPrinterTopic(topic) ? CHARSET : StandardCharsets.UTF_8;
        return payload.getBytes(charset);
    }

    private static boolean isEncodable(int codePoint) {
        return codePoint >= 0 && codePoint < ENCODABLE.length && ENCODABLE[codePoint];
    }
}
