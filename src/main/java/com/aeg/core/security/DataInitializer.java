package com.aeg.core.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username:}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("Admin bootstrap skipped: APP_SECURITY_ADMIN_USERNAME/PASSWORD not set.");
            return;
        }

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            String adminName = adminUsername.contains("@")
                    ? adminUsername.substring(0, adminUsername.indexOf('@'))
                    : adminUsername;
            User admin = User.builder()
                    .username(adminUsername)
                    .name(adminName)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Initial admin user created for username={}", adminUsername);
        }
    }
}
