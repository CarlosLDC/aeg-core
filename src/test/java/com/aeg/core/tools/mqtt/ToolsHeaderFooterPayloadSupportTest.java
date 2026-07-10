package com.aeg.core.tools.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ToolsHeaderFooterPayloadSupportTest {

    @Test
    void parseLinesTrimsAndDropsBlankLines() {
        assertEquals(
                List.of("LINEA 1", "LINEA 2"),
                ToolsHeaderFooterPayloadSupport.parseLines(" LINEA 1 \n\nLINEA 2\r\n"));
    }

    @Test
    void validateLinesRejectsFooterOverflow() {
        assertThrows(
                ToolsMqttOperationException.class,
                () -> ToolsHeaderFooterPayloadSupport.validateLines(
                        List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                        ToolsMqttConstants.FOOTER_MAX_LINES,
                        "pie de página"));
    }
}
