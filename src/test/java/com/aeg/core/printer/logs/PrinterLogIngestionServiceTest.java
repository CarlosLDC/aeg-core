package com.aeg.core.printer.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import com.aeg.core.printer.PrinterRepository;

@ExtendWith(MockitoExtension.class)
class PrinterLogIngestionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PrinterRepository printerRepository;

    private PrinterLogIngestionService service;

    @BeforeEach
    void setUp() {
        service = new PrinterLogIngestionService(jdbcTemplate, printerRepository);
    }

    @Test
    void cachesNegativePrinterIdLookupsToAvoidRepeatedQueries() {
        when(printerRepository.findIdByMacAddressCompact("206EF1884C68")).thenReturn(Optional.empty());
        when(printerRepository.findByMacAddressCompact("206EF1884C68")).thenReturn(Optional.empty());

        assertThat(service.resolvePrinterId("206EF1884C68")).isNull();
        assertThat(service.resolvePrinterId("206EF1884C68")).isNull();

        verify(printerRepository, times(1)).findIdByMacAddressCompact("206EF1884C68");
    }

    @Test
    void truncatesOversizedPayloadAndDrainsMultipleBatches() {
        String hugePayload = "X".repeat(10_000);
        for (int i = 0; i < 600; i++) {
            service.enqueue(new PrinterLogEntry(
                    "UNKNOWN",
                    "/206EF1884C68/AEG_Fiscal/Integracion/DebugMQTT",
                    hugePayload,
                    Instant.now()));
        }

        assertThat(service.queueSize()).isEqualTo(600);
        service.flushBatch();
        assertThat(service.queueSize()).isZero();

        verify(jdbcTemplate, times(2)).batchUpdate(
                anyString(),
                any(),
                anyInt(),
                any(ParameterizedPreparedStatementSetter.class));
    }
}
