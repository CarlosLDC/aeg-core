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

    private static final int QUEUE_CAPACITY = 4_000;
    private static final int MAX_BATCH_DRAIN = 500;
    private static final int MAX_BATCHES_PER_TICK = 8;
    private static final int MAX_PAYLOAD_LENGTH = 8_192;
    private static final int MAX_CACHE_ENTRIES = 2_000;
    private static final long NEGATIVE_CACHE_TTL_MS = 60_000L;

    private record CachedPrinterId(Long printerId, long resolvedAtMs) {}

    private final BlockingQueue<PrinterLogEntry> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Map<String, CachedPrinterId> macToPrinterIdCache = new ConcurrentHashMap<>();

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
        PrinterLogEntry sanitized = sanitizeEntry(entry);
        boolean offered = queue.offer(sanitized);
        if (!offered) {
            log.warn("Printer log queue is full ({}); dropping log for mac={}", QUEUE_CAPACITY, sanitized.mac());
        }
        return offered;
    }

    private static PrinterLogEntry sanitizeEntry(PrinterLogEntry entry) {
        String payload = entry.payload();
        if (payload != null && payload.length() > MAX_PAYLOAD_LENGTH) {
            return new PrinterLogEntry(
                    entry.mac(),
                    entry.topic(),
                    payload.substring(0, MAX_PAYLOAD_LENGTH),
                    entry.recordedAt());
        }
        return entry;
    }

    public int queueSize() {
        return queue.size();
    }

    @Scheduled(fixedDelay = 1000)
    public void flushBatch() {
        if (queue.isEmpty()) {
            return;
        }
        List<PrinterLogEntry> batch = new ArrayList<>(MAX_BATCH_DRAIN);
        for (int i = 0; i < MAX_BATCHES_PER_TICK && !queue.isEmpty(); i++) {
            batch.clear();
            queue.drainTo(batch, MAX_BATCH_DRAIN);
            if (batch.isEmpty()) {
                break;
            }
            try {
                persistBatch(batch);
            } catch (Exception ex) {
                log.error("Failed to persist printer log batch of size {}: {}", batch.size(), ex.getMessage(), ex);
                break;
            }
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
        if ("UNKNOWN".equals(normalized)) {
            return null;
        }

        long now = System.currentTimeMillis();
        CachedPrinterId cached = macToPrinterIdCache.get(normalized);
        if (cached != null) {
            if (cached.printerId() != null || (now - cached.resolvedAtMs()) < NEGATIVE_CACHE_TTL_MS) {
                return cached.printerId();
            }
        }

        if (macToPrinterIdCache.size() >= MAX_CACHE_ENTRIES) {
            macToPrinterIdCache.clear();
        }

        Long resolved = printerRepository.findIdByMacAddressCompact(normalized)
                .or(() -> printerRepository.findByMacAddressCompact(normalized).map(p -> p.getId()))
                .orElse(null);
        macToPrinterIdCache.put(normalized, new CachedPrinterId(resolved, now));
        return resolved;
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
