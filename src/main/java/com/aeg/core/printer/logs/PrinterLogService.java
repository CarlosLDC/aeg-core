package com.aeg.core.printer.logs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PrinterLogService {

    private static final int DELETE_CHUNK_SIZE = 2500;

    private final JdbcTemplate jdbcTemplate;

    public PrinterLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Search printer logs with date and time range filters, optional text search, and pagination.
     */
    @Transactional(readOnly = true)
    public PrinterLogsPageResponse findLogs(
            Long printerId,
            String mac,
            Instant from,
            Instant to,
            String search,
            int page,
            int limit,
            String order
    ) {
        int effectiveLimit = Math.min(Math.max(1, limit), 200);
        int effectivePage = Math.max(0, page);
        int offset = effectivePage * effectiveLimit;
        boolean isAsc = "asc".equalsIgnoreCase(order);

        List<Object> countParams = new ArrayList<>();
        List<Object> queryParams = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");

        if (printerId != null) {
            where.append(" AND printer_id = ? ");
            countParams.add(printerId);
            queryParams.add(printerId);
        } else if (mac != null && !mac.isBlank()) {
            where.append(" AND mac = ? ");
            String compact = mac.trim().toUpperCase().replace(":", "");
            countParams.add(compact);
            queryParams.add(compact);
        }

        if (from != null) {
            where.append(" AND recorded_at >= ? ");
            countParams.add(Timestamp.from(from));
            queryParams.add(Timestamp.from(from));
        }

        if (to != null) {
            where.append(" AND recorded_at <= ? ");
            countParams.add(Timestamp.from(to));
            queryParams.add(Timestamp.from(to));
        }

        if (search != null && !search.isBlank()) {
            where.append(" AND (payload ILIKE ? OR topic ILIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            countParams.add(pattern);
            countParams.add(pattern);
            queryParams.add(pattern);
            queryParams.add(pattern);
        }

        String countSql = "SELECT COUNT(*) FROM public.printer_mqtt_logs " + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());
        long totalCount = total != null ? total : 0L;

        String sortDir = isAsc ? "ASC" : "DESC";
        String selectSql = "SELECT id, recorded_at, printer_id, mac, topic, payload " +
                " FROM public.printer_mqtt_logs " + where +
                " ORDER BY recorded_at " + sortDir + ", id " + sortDir +
                " LIMIT ? OFFSET ? ";

        queryParams.add(effectiveLimit);
        queryParams.add(offset);

        List<PrinterMqttLogItem> logs = jdbcTemplate.query(selectSql, (rs, rowNum) -> mapRow(rs), queryParams.toArray());

        return new PrinterLogsPageResponse(
                printerId,
                mac,
                totalCount,
                effectivePage,
                effectiveLimit,
                logs
        );
    }

    /**
     * Delete logs for a specific printer within a date range or before a cutoff date.
     * Uses batched deletion to prevent table locks and transaction saturation.
     */
    public int deleteLogs(Long printerId, String mac, Instant from, Instant to, Instant before) {
        if (printerId == null && (mac == null || mac.isBlank())) {
            throw new IllegalArgumentException("Must specify printerId or mac to delete printer logs");
        }

        Instant effectiveBefore = before;
        Instant effectiveFrom = from;
        Instant effectiveTo = to;

        if (effectiveBefore != null && effectiveTo == null) {
            effectiveTo = effectiveBefore;
        }

        if (effectiveFrom == null && effectiveTo == null) {
            throw new IllegalArgumentException("Must specify a date filter (before, or from/to) to delete logs");
        }

        String targetCol = printerId != null ? "printer_id" : "mac";
        Object targetVal = printerId != null ? printerId : mac.trim().toUpperCase().replace(":", "");

        StringBuilder dateCondition = new StringBuilder();
        List<Object> baseParams = new ArrayList<>();
        baseParams.add(targetVal);

        if (effectiveFrom != null) {
            dateCondition.append(" AND recorded_at >= ? ");
            baseParams.add(Timestamp.from(effectiveFrom));
        }
        if (effectiveTo != null) {
            dateCondition.append(" AND recorded_at <= ? ");
            baseParams.add(Timestamp.from(effectiveTo));
        }

        String deleteChunkSql = """
            WITH to_delete AS (
                SELECT id, recorded_at
                FROM public.printer_mqtt_logs
                WHERE %s = ? %s
                LIMIT ?
            )
            DELETE FROM public.printer_mqtt_logs p
            USING to_delete
            WHERE p.id = to_delete.id AND p.recorded_at = to_delete.recorded_at
        """.formatted(targetCol, dateCondition.toString());

        int totalDeleted = 0;
        int deletedInBatch;
        do {
            List<Object> chunkParams = new ArrayList<>(baseParams);
            chunkParams.add(DELETE_CHUNK_SIZE);
            deletedInBatch = jdbcTemplate.update(deleteChunkSql, chunkParams.toArray());
            totalDeleted += deletedInBatch;
        } while (deletedInBatch == DELETE_CHUNK_SIZE);

        log.info("Deleted {} logs for {}={} (from={}, to={})", totalDeleted, targetCol, targetVal, effectiveFrom, effectiveTo);
        return totalDeleted;
    }

    /**
     * Global deletion of logs older than cutoff date.
     * Drops complete monthly partitions when all dates fall strictly before the cutoff,
     * and deletes remaining rows in batches.
     */
    public GlobalDeleteResult deleteGlobalLogsBefore(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("Cutoff date is required for global log deletion");
        }

        List<String> droppedPartitions = new ArrayList<>();
        int chunkDeleted = 0;

        // Step 1: Detect and drop whole partitions if completely before cutoff
        List<PartitionMeta> partitions = listMonthlyPartitions();
        for (PartitionMeta part : partitions) {
            if (part.upperBound() != null && !part.upperBound().isAfter(cutoff)) {
                try {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS public." + part.tableName() + ";");
                    droppedPartitions.add(part.tableName());
                    log.info("Dropped expired printer log partition: {}", part.tableName());
                } catch (Exception ex) {
                    log.error("Failed to drop partition {}: {}", part.tableName(), ex.getMessage());
                }
            }
        }

        // Step 2: Delete any remaining records in active partitions or default partition before cutoff
        String deleteChunkSql = """
            WITH to_delete AS (
                SELECT id, recorded_at
                FROM public.printer_mqtt_logs
                WHERE recorded_at <= ?
                LIMIT ?
            )
            DELETE FROM public.printer_mqtt_logs p
            USING to_delete
            WHERE p.id = to_delete.id AND p.recorded_at = to_delete.recorded_at
        """;

        int deletedInBatch;
        do {
            deletedInBatch = jdbcTemplate.update(deleteChunkSql, Timestamp.from(cutoff), DELETE_CHUNK_SIZE);
            chunkDeleted += deletedInBatch;
        } while (deletedInBatch == DELETE_CHUNK_SIZE);

        log.info("Global log cleanup before {}: dropped {} partitions, deleted {} chunked records",
                cutoff, droppedPartitions.size(), chunkDeleted);

        return new GlobalDeleteResult(chunkDeleted, droppedPartitions);
    }

    public record GlobalDeleteResult(int chunkDeleted, List<String> droppedPartitions) {}

    private record PartitionMeta(String tableName, Instant upperBound) {}

    private List<PartitionMeta> listMonthlyPartitions() {
        String sql = """
            SELECT c.relname as table_name
            FROM pg_class c
            JOIN pg_inherits i ON i.inhrelid = c.oid
            JOIN pg_class p ON p.oid = i.inhparent
            WHERE p.relname = 'printer_mqtt_logs'
              AND c.relname LIKE 'printer_mqtt_logs_y%m%'
            ORDER BY c.relname
        """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String name = rs.getString("table_name");
                // Expected format: printer_mqtt_logs_yYYYYmMM
                Instant upperBound = parsePartitionUpperDate(name);
                return new PartitionMeta(name, upperBound);
            });
        } catch (Exception ex) {
            log.warn("Could not query partition metadata: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private static Instant parsePartitionUpperDate(String tableName) {
        try {
            String[] parts = tableName.split("_y");
            if (parts.length == 2) {
                String[] ym = parts[1].split("m");
                int year = Integer.parseInt(ym[0]);
                int month = Integer.parseInt(ym[1]);
                // upper bound is first day of next month
                int nextMonth = month == 12 ? 1 : month + 1;
                int nextYear = month == 12 ? year + 1 : year;
                return Instant.parse(String.format("%04d-%02d-01T00:00:00Z", nextYear, nextMonth));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static PrinterMqttLogItem mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("recorded_at");
        long id = rs.getLong("id");
        Long printerId = rs.getObject("printer_id", Long.class);
        String mac = rs.getString("mac");
        String topic = rs.getString("topic");
        String payload = rs.getString("payload");

        return new PrinterMqttLogItem(
                id,
                ts != null ? ts.toInstant() : null,
                printerId,
                mac,
                topic,
                payload
        );
    }
}
