package com.aeg.core.printer.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class PrinterLogServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PrinterLogService service;

    @BeforeEach
    void setUp() {
        service = new PrinterLogService(jdbcTemplate);
    }

    @Test
    void findLogsQueriesWithPagingAndCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(42L);

        PrinterMqttLogItem sample = new PrinterMqttLogItem(
                1L, Instant.now(), 10L, "206EF1884C68",
                "/206EF1884C68/AEG_Fiscal/Integracion/DebugMQTT", "{\"test\":1}"
        );

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sample));

        PrinterLogsPageResponse response = service.findLogs(
                10L, "20:6E:F1:88:4C:68",
                Instant.now().minusSeconds(3600), Instant.now(),
                "test", 0, 50, "desc"
        );

        assertThat(response.total()).isEqualTo(42L);
        assertThat(response.logs()).hasSize(1);
        assertThat(response.logs().get(0).mac()).isEqualTo("206EF1884C68");
    }

    @Test
    void deleteLogsExecutesChunkedBatch() {
        // First batch returns 2500 (full chunk), second returns 100 (done)
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(2500)
                .thenReturn(100);

        int deleted = service.deleteLogs(10L, null, null, null, Instant.now());
        assertThat(deleted).isEqualTo(2600);
    }
}
