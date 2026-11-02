package com.aeg.core.tools.mqtt;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Recuerda las actualizaciones de firmware lanzadas desde Tools y verifica en segundo plano si la
 * impresora quedó con una versión nueva.
 * <ul>
 *   <li>Versión distinta a la previa: se guarda en la base de datos (ver {@code syncPrinterFirmware}).</li>
 *   <li>Misma versión: la actualización no se aplicó; no se cambia nada.</li>
 *   <li>Sin respuesta tras agotar los intentos: se marca la impresora con
 *       {@code firmwareUpdateUnverified} para mostrar una alerta.</li>
 * </ul>
 */
@Slf4j
@Service
public class FirmwareUpdateTracker {

    private final FirmwareUpdateAttemptRepository attemptRepository;
    private final PrinterRepository printerRepository;
    private final BrokerMigrationService brokerMigrationService;
    private final long initialDelaySeconds;
    private final long retryIntervalSeconds;
    private final int maxAttempts;

    public FirmwareUpdateTracker(
            FirmwareUpdateAttemptRepository attemptRepository,
            PrinterRepository printerRepository,
            BrokerMigrationService brokerMigrationService,
            @Value("${app.firmware-update.initial-delay-seconds:90}") long initialDelaySeconds,
            @Value("${app.firmware-update.retry-interval-seconds:30}") long retryIntervalSeconds,
            @Value("${app.firmware-update.max-attempts:10}") int maxAttempts) {
        this.attemptRepository = attemptRepository;
        this.printerRepository = printerRepository;
        this.brokerMigrationService = brokerMigrationService;
        this.initialDelaySeconds = initialDelaySeconds;
        this.retryIntervalSeconds = retryIntervalSeconds;
        this.maxAttempts = maxAttempts;
    }

    /** Versión de firmware registrada actualmente en la base de datos (puede ser null). */
    public String currentVersion(Long printerId) {
        return printerRepository.findById(printerId).map(Printer::getVersionFirmware).orElse(null);
    }

    /** Registra una actualización recién lanzada y limpia la alerta anterior de la impresora. */
    public void register(Long printerId, String previousVersion) {
        List<FirmwareUpdateAttempt> pending =
                attemptRepository.findByPrinterIdAndStatus(printerId, FirmwareUpdateAttempt.Status.PENDING);
        OffsetDateTime now = OffsetDateTime.now();
        for (FirmwareUpdateAttempt old : pending) {
            old.setStatus(FirmwareUpdateAttempt.Status.SUPERSEDED);
            old.setFinishedAt(now);
        }
        attemptRepository.saveAll(pending);

        FirmwareUpdateAttempt attempt = new FirmwareUpdateAttempt();
        attempt.setPrinterId(printerId);
        attempt.setStatus(FirmwareUpdateAttempt.Status.PENDING);
        attempt.setPreviousVersion(previousVersion);
        attempt.setRequestedAt(now);
        attempt.setNextCheckAt(now.plusSeconds(initialDelaySeconds));
        attemptRepository.save(attempt);
        setUnverified(printerId, false);
        log.info("Registered firmware update attempt for printer {} (previous version {})", printerId, previousVersion);
    }

    /** Quita la alerta cuando la impresora respondió a una consulta de versión. */
    public void clearUnverified(Long printerId) {
        setUnverified(printerId, false);
    }

    @Scheduled(fixedDelayString = "${app.firmware-update.poll-interval-ms:15000}")
    public void verifyDueAttempts() {
        List<FirmwareUpdateAttempt> due = attemptRepository.findByStatusAndNextCheckAtLessThanEqual(
                FirmwareUpdateAttempt.Status.PENDING, OffsetDateTime.now());
        for (FirmwareUpdateAttempt attempt : due) {
            try {
                verify(attempt);
            } catch (RuntimeException ex) {
                log.warn("Unexpected error verifying firmware update attempt {}: {}", attempt.getId(), ex.getMessage());
            }
        }
    }

    void verify(FirmwareUpdateAttempt attempt) {
        Long printerId = attempt.getPrinterId();
        attempt.setAttempts(attempt.getAttempts() + 1);

        String version = null;
        try {
            version = brokerMigrationService.readFirmwareVersionInBackground(printerId);
            attempt.setGotReply(true);
        } catch (RuntimeException ex) {
            log.info("Firmware update verification {}/{} for printer {} got no usable reply: {}",
                    attempt.getAttempts(), maxAttempts, printerId, ex.getMessage());
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (version != null && (attempt.getPreviousVersion() == null || !version.equals(attempt.getPreviousVersion()))) {
            attempt.setStatus(FirmwareUpdateAttempt.Status.CONFIRMED);
            attempt.setFinishedAt(now);
            setUnverified(printerId, false);
        } else if (attempt.getAttempts() >= maxAttempts) {
            attempt.setFinishedAt(now);
            if (attempt.isGotReply()) {
                attempt.setStatus(FirmwareUpdateAttempt.Status.FAILED);
                setUnverified(printerId, false);
            } else {
                attempt.setStatus(FirmwareUpdateAttempt.Status.NO_RESPONSE);
                setUnverified(printerId, true);
            }
        } else {
            attempt.setNextCheckAt(now.plusSeconds(retryIntervalSeconds));
        }
        attemptRepository.save(attempt);
    }

    private void setUnverified(Long printerId, boolean value) {
        printerRepository.findById(printerId).ifPresent(printer -> {
            if (printer.isFirmwareUpdateUnverified() != value) {
                printer.setFirmwareUpdateUnverified(value);
                printerRepository.save(printer);
            }
        });
    }
}