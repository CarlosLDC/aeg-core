package com.aeg.core.tools.mqtt;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmwareUpdateAttemptRepository extends JpaRepository<FirmwareUpdateAttempt, Long> {

    List<FirmwareUpdateAttempt> findByPrinterIdAndStatus(Long printerId, FirmwareUpdateAttempt.Status status);

    List<FirmwareUpdateAttempt> findByStatusAndNextCheckAtLessThanEqual(
            FirmwareUpdateAttempt.Status status, OffsetDateTime now);
}
