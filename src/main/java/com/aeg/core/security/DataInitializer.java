package com.aeg.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.security.admin.username}")
    private String adminUsername;

    @org.springframework.beans.factory.annotation.Value("${app.security.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Administrador inicial creado con éxito.");
        } else {
            User admin = userRepository.findByUsername(adminUsername).get();
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("🔄 Credenciales de administrador sincronizadas desde configuración.");
        }
    }
}
