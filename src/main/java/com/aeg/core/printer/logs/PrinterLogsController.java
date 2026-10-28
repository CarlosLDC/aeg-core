package com.aeg.core.printer.logs;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.security.SecurityScopeService;

@RestController
@RequestMapping("/api")
public class PrinterLogsController {

    private final PrinterRepository printerRepository;
    private final PrinterLogService printerLogService;
    private final SecurityScopeService securityScopeService;

    public PrinterLogsController(
            PrinterRepository printerRepository,
            PrinterLogService printerLogService,
            SecurityScopeService securityScopeService) {
        this.printerRepository = printerRepository;
        this.printerLogService = printerLogService;
        this.securityScopeService = securityScopeService;
    }

    @GetMapping("/printers/{id}/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrinterLogsPageResponse> getPrinterLogs(
            @PathVariable Long id,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Printer printer = printerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impresora no encontrada"));
        securityScopeService.assertPrinterInScope(printer);

        PrinterLogsPageResponse response = printerLogService.findLogs(
                id,
                printer.getMacAddress(),
                from,
                to,
                q,
                page,
                limit,
                order
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/printers/{id}/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePrinterLogs(
            @PathVariable Long id,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Instant before
    ) {
        Printer printer = printerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impresora no encontrada"));

        int deleted = printerLogService.deleteLogs(id, printer.getMacAddress(), from, to, before);
        return ResponseEntity.ok(Map.of(
                "deletedCount", deleted,
                "message", "Se eliminaron " + deleted + " logs de la impresora correctamente."
        ));
    }

    @DeleteMapping("/admin/printer-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteGlobalLogs(
            @RequestParam Instant before
    ) {
        PrinterLogService.GlobalDeleteResult result = printerLogService.deleteGlobalLogsBefore(before);
        return ResponseEntity.ok(Map.of(
                "deletedCount", result.chunkDeleted(),
                "droppedPartitions", result.droppedPartitions(),
                "message", "Limpieza de logs completada antes de " + before
        ));
    }
}
