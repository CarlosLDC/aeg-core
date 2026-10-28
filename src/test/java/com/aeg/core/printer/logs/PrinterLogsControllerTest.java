package com.aeg.core.printer.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;

@ExtendWith(MockitoExtension.class)
class PrinterLogsControllerTest {

    @Mock
    private PrinterRepository printerRepository;

    @Mock
    private PrinterLogService printerLogService;

    @Mock
    private SecurityScopeService securityScopeService;

    private PrinterLogsController controller;

    @BeforeEach
    void setUp() {
        controller = new PrinterLogsController(printerRepository, printerLogService, securityScopeService);
    }

    @Test
    void getPrinterLogsValidatesScopeAndReturnsResponse() {
        Printer printer = new Printer();
        printer.setId(5L);
        printer.setMacAddress("20:6E:F1:88:4C:68");

        when(printerRepository.findById(5L)).thenReturn(Optional.of(printer));

        PrinterLogsPageResponse expected = new PrinterLogsPageResponse(
                5L, "20:6E:F1:88:4C:68", 0L, 0, 50, List.of()
        );
        when(printerLogService.findLogs(eq(5L), eq("20:6E:F1:88:4C:68"), any(), any(), any(), eq(0), eq(50), eq("desc")))
                .thenReturn(expected);

        ResponseEntity<PrinterLogsPageResponse> response = controller.getPrinterLogs(
                5L, null, null, null, 0, 50, "desc"
        );

        verify(securityScopeService).assertPrinterInScope(printer);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void deletePrinterLogsCallsService() {
        Printer printer = new Printer();
        printer.setId(5L);
        printer.setMacAddress("20:6E:F1:88:4C:68");

        when(printerRepository.findById(5L)).thenReturn(Optional.of(printer));
        when(printerLogService.deleteLogs(eq(5L), eq("20:6E:F1:88:4C:68"), any(), any(), any()))
                .thenReturn(150);

        var response = controller.deletePrinterLogs(5L, null, null, Instant.now());
        assertThat(response.getBody().get("deletedCount")).isEqualTo(150);
    }
}
