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

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "segar12345@gmail.com";
        
        if (userRepository.findByUsername(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .username(adminEmail)
                    .password(passwordEncoder.encode("aeg-r1"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Administrador inicial creado con éxito.");
        } else {
            // Opcional: Asegurar que la contraseña sea la correcta si ya existe
            User admin = userRepository.findByUsername(adminEmail).get();
            admin.setPassword(passwordEncoder.encode("aeg-r1"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("🔄 Credenciales de administrador sincronizadas.");
        }
    }
}
