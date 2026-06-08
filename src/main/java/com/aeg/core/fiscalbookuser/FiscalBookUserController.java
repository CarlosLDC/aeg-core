package com.aeg.core.fiscalbookuser;

import com.aeg.core.employee.Employee;
import com.aeg.core.employee.EmployeeRepository;
import com.aeg.core.security.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/fiscal-book-users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FiscalBookUserController {

    private final FiscalBookUserRepository fiscalBookUserRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegistrationRequest request) {
        String name = normalizeName(request.getName());
        String email = normalizeEmail(request.getEmail());
        if (name == null || email == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (fiscalBookUserRepository.findByUsername(email).isPresent()
                || userRepository.findByUsername(email).isPresent()) {
            return ResponseEntity.status(409).build();
        }

        FiscalBookRole role;
        try {
            role = FiscalBookRole.valueOf(request.getRole().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        Employee employee = null;
        if (role == FiscalBookRole.FISCAL_TECHNICIAN) {
            if (request.getEmployeeId() == null) {
                return ResponseEntity.badRequest().build();
            }
            employee = employeeRepository.findById(request.getEmployeeId()).orElse(null);
            if (employee == null) {
                return ResponseEntity.notFound().build();
            }
        } else if (request.getEmployeeId() != null) {
            return ResponseEntity.badRequest().build();
        }

        FiscalBookUser user = FiscalBookUser.builder()
                .username(email)
                .name(name)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .employeeId(employee != null ? employee.getId() : null)
                .employee(employee)
                .enabled(true)
                .build();

        FiscalBookUser saved = fiscalBookUserRepository.save(user);
        return ResponseEntity.created(URI.create("/api/admin/fiscal-book-users/" + saved.getId()))
                .body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                fiscalBookUserRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return fiscalBookUserRepository.findById(id)
                .map(user -> ResponseEntity.ok(toResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return fiscalBookUserRepository.findById(id)
                .map(existing -> updateExisting(existing, request))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return fiscalBookUserRepository.findById(id)
                .map(user -> {
                    fiscalBookUserRepository.delete(user);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<UserResponse> updateExisting(FiscalBookUser existing, UserUpdateRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            existing.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = normalizeEmail(request.getEmail());
            if (email == null) {
                return ResponseEntity.badRequest().build();
            }
            var foundFiscal = fiscalBookUserRepository.findByUsername(email);
            if (foundFiscal.isPresent() && !foundFiscal.get().getId().equals(existing.getId())) {
                return ResponseEntity.status(409).build();
            }
            if (userRepository.findByUsername(email).isPresent()) {
                return ResponseEntity.status(409).build();
            }
            existing.setUsername(email);
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        FiscalBookRole role = existing.getRole();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = FiscalBookRole.valueOf(request.getRole().toUpperCase(Locale.ROOT));
                existing.setRole(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long employeeId = existing.getEmployeeId();
        if (request.getEmployeeId() != null) {
            employeeId = request.getEmployeeId();
        }
        if (role == FiscalBookRole.FISCAL_TECHNICIAN) {
            if (employeeId == null) {
                return ResponseEntity.badRequest().build();
            }
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                return ResponseEntity.notFound().build();
            }
            existing.setEmployee(employee);
            existing.setEmployeeId(employee.getId());
        } else {
            existing.setEmployee(null);
            existing.setEmployeeId(null);
        }

        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }

        return ResponseEntity.ok(toResponse(fiscalBookUserRepository.save(existing)));
    }

    @Data
    public static class UserRegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long employeeId;
    }

    @Data
    public static class UserUpdateRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long employeeId;
        private Boolean enabled;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private FiscalBookRole role;
        private Long employeeId;
        private Boolean enabled;

        public UserResponse(Long id, String name, String email, FiscalBookRole role, Long employeeId, Boolean enabled) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.employeeId = employeeId;
            this.enabled = enabled;
        }
    }

    private UserResponse toResponse(FiscalBookUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getRole(),
                user.getEmployeeId(),
                user.isEnabled()
        );
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null) return null;
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || !email.contains("@")) return null;
        return email;
    }

    private String normalizeName(String rawName) {
        if (rawName == null) return null;
        String name = rawName.trim();
        return name.isBlank() ? null : name;
    }
}
