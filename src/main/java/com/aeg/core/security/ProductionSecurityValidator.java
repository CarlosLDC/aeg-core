package com.aeg.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * En despliegue (sin perfil dev/test) exige secretos explícitos por entorno.
 */
@Component
@Profile("!test & !dev")
@Slf4j
public class ProductionSecurityValidator implements ApplicationRunner {

    private static final String DEFAULT_OTA_TOKEN = "changeme-insecure-token";
    private static final String DEFAULT_QR_SECRET = "AeGsGrAsFeCh2024";

    @Value("${app.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.admin.username:}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

    @Value("${app.ota.download-token:}")
    private String otaDownloadToken;

    @Value("${app.annual-inspection.qr.secret:}")
    private String qrSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "APP_SECURITY_JWT_SECRET is required in production (min. 32 characters recommended).");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "APP_SECURITY_JWT_SECRET must be at least 32 characters.");
        }
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_SECURITY_ADMIN_USERNAME and APP_SECURITY_ADMIN_PASSWORD are required in production.");
        }
        if (otaDownloadToken == null || otaDownloadToken.isBlank()
                || DEFAULT_OTA_TOKEN.equals(otaDownloadToken)) {
            log.warn(
                    "APP_OTA_DOWNLOAD_TOKEN is missing or still using the insecure default; configure a strong random secret in production.");
        }
        if (qrSecret == null || qrSecret.isBlank()
                || DEFAULT_QR_SECRET.equals(qrSecret)) {
            log.warn(
                    "ANNUAL_INSPECTION_QR_SECRET is missing or still using the default secret; rotate and configure via environment secrets.");
        }
    }
}
