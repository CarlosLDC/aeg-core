package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class VenezuelaNaiveUnixTimestampTest {

    @Test
    void subtractsFourHoursFromUtcEpochSeconds() {
        long utcEpochSeconds = 1_782_273_600L;

        assertThat(VenezuelaNaiveUnixTimestamp.fromInstant(Instant.ofEpochSecond(utcEpochSeconds)))
                .isEqualTo(1_782_259_200L);
    }
}
