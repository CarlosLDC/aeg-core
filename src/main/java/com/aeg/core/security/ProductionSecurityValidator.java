package com.aeg.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * En despliegue (sin perfil dev/test) exige secretos explícitos por entorno.
 */
@Component
@Profile("!test & !dev")
public class ProductionSecurityValidator implements ApplicationRunner {

    @Value("${app.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.admin.username:}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

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
    }
}
