package com.aeg.core.tools.mqtt;

import java.util.Arrays;
import java.util.List;

import com.aeg.core.fiscal.FiscalTicketLatin2;

final class ToolsHeaderFooterPayloadSupport {

    private ToolsHeaderFooterPayloadSupport() {
    }

    static List<String> parseLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(FiscalTicketLatin2::normalizeFiscalTicketText)
                .toList();
    }

    static void validateLines(List<String> lines, int maxLines, String label) {
        if (lines.size() > maxLines) {
            throw new ToolsMqttOperationException(
                    String.format("El %s admite como máximo %d líneas.", label, maxLines));
        }
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).length() > ToolsMqttConstants.HEADER_FOOTER_MAX_LINE_LENGTH) {
                throw new ToolsMqttOperationException(String.format(
                        "La línea %d del %s supera los %d caracteres.",
                        index + 1,
                        label,
                        ToolsMqttConstants.HEADER_FOOTER_MAX_LINE_LENGTH));
            }
        }
    }
}
