package com.aeg.core.printer.logs;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aeg.core.printer.PrinterRepository;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * High-performance, non-blocking ingestion service for printer MQTT logs.
 * Buffers incoming log lines in memory and writes them in batches to the partitioned PostgreSQL table.
 * Prevents HikariCP connection pool starvation and DB CPU/WAL spikes during heavy log bursts.
 */
@Service
@Slf4j
public class PrinterLogIngestionService {

    private static final int QUEUE_CAPACITY = 20_000;
    private static final int MAX_BATCH_DRAIN = 500;

    private final BlockingQueue<PrinterLogEntry> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Map<String, Long> macToPrinterIdCache = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;
    private final PrinterRepository printerRepository;

    public PrinterLogIngestionService(JdbcTemplate jdbcTemplate, PrinterRepository printerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.printerRepository = printerRepository;
    }

    public boolean enqueue(PrinterLogEntry entry) {
        if (entry == null) {
            return false;
        }
        boolean offered = queue.offer(entry);
        if (!offered) {
            log.warn("Printer log queue is full ({}); dropping log for mac={}", QUEUE_CAPACITY, entry.mac());
        }
        return offered;
    }

    public int queueSize() {
        return queue.size();
    }

    @Scheduled(fixedDelay = 1000)
    public void flushBatch() {
        if (queue.isEmpty()) {
            return;
        }
        List<PrinterLogEntry> batch = new ArrayList<>(Math.min(queue.size(), MAX_BATCH_DRAIN));
        queue.drainTo(batch, MAX_BATCH_DRAIN);
        if (batch.isEmpty()) {
            return;
        }

        try {
            persistBatch(batch);
        } catch (Exception ex) {
            log.error("Failed to persist printer log batch of size {}: {}", batch.size(), ex.getMessage(), ex);
        }
    }

    private void persistBatch(List<PrinterLogEntry> batch) {
        String sql = """
            INSERT INTO public.printer_mqtt_logs (recorded_at, printer_id, mac, topic, payload)
            VALUES (?, ?, ?, ?, ?)
        """;

        jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, entry) -> {
            ps.setTimestamp(1, Timestamp.from(entry.recordedAt()));
            Long printerId = resolvePrinterId(entry.mac());
            if (printerId != null) {
                ps.setLong(2, printerId);
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            ps.setString(3, entry.mac());
            ps.setString(4, entry.topic());
            ps.setString(5, entry.payload());
        });

        if (log.isDebugEnabled()) {
            log.debug("Persisted {} printer logs to database", batch.size());
        }
    }

    public Long resolvePrinterId(String compactMac) {
        if (compactMac == null || compactMac.isBlank()) {
            return null;
        }
        String normalized = compactMac.trim().toUpperCase();
        return macToPrinterIdCache.computeIfAbsent(normalized, mac ->
                printerRepository.findByMacAddressCompact(mac)
                        .map(p -> p.getId())
                        .orElse(null)
        );
    }

    public void invalidateCache(String compactMac) {
        if (compactMac != null) {
            macToPrinterIdCache.remove(compactMac.trim().toUpperCase());
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Flushing remaining printer logs on shutdown (queue size={})...", queue.size());
        while (!queue.isEmpty()) {
            List<PrinterLogEntry> batch = new ArrayList<>(MAX_BATCH_DRAIN);
            queue.drainTo(batch, MAX_BATCH_DRAIN);
            if (!batch.isEmpty()) {
                try {
                    persistBatch(batch);
                } catch (Exception ex) {
                    log.error("Error flushing logs on shutdown: {}", ex.getMessage());
                }
            }
        }
        log.info("Printer log queue successfully flushed.");
    }
}
