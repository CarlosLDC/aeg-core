package com.aeg.core.security;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.modificationrequest.ModificationRequestRepository;

import java.util.List;
import java.net.URI;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final DistributorRepository distributorRepository;
    private final ModificationRequestRepository modificationRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegistrationRequest request) {
        String name = normalizeName(request.getName());
        String email = normalizeEmail(request.getEmail());
        if (name == null || email == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (userRepository.findByUsername(email).isPresent()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        String nationalId = normalizeNationalId(request.getNationalId());
        Long distributorId = request.getDistributorId();

        if (role == Role.TECHNICIAN) {
            if (nationalId == null || distributorId == null) {
                return ResponseEntity.badRequest().build();
            }
            if (userRepository.findByNationalId(nationalId).isPresent()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
            }
            if (distributorRepository.findById(distributorId).isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }
        } else if (role == Role.ADMIN || role == Role.SENIAT) {
            nationalId = null;
            distributorId = null;
        } else {
            return ResponseEntity.badRequest().build();
        }

        Distributor distributor = distributorId != null
                ? distributorRepository.findById(distributorId).orElse(null)
                : null;

        User user = User.builder()
                .username(email)
                .name(name)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .branchId(null)
                .branch(null)
                .distributorId(distributorId)
                .distributor(distributor)
                .nationalId(nationalId)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        URI location = URI.create("/api/admin/users/" + saved.getId());
        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        var maybe = userRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        User existing = maybe.get();
        if (request.getName() != null && !request.getName().isBlank()) {
            existing.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = normalizeEmail(request.getEmail());
            if (email == null) {
                return ResponseEntity.badRequest().build();
            }
            var found = userRepository.findByUsername(email);
            if (found.isPresent() && !found.get().getId().equals(existing.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
            }
            existing.setUsername(email);
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        Role role = existing.getRole();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().toUpperCase(Locale.ROOT));
                existing.setRole(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        if (role == Role.TECHNICIAN) {
            String nationalId = request.getNationalId() != null
                    ? normalizeNationalId(request.getNationalId())
                    : existing.getNationalId();
            Long distributorId = request.getDistributorId() != null
                    ? request.getDistributorId()
                    : existing.getDistributorId();
            if (nationalId == null || distributorId == null) {
                return ResponseEntity.badRequest().build();
            }
            if (userRepository.existsByNationalIdAndIdNot(nationalId, existing.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
            }
            if (distributorRepository.findById(distributorId).isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }
            existing.setNationalId(nationalId);
            existing.setDistributorId(distributorId);
            existing.setDistributor(distributorRepository.findById(distributorId).orElse(null));
            existing.setBranchId(null);
            existing.setBranch(null);
        } else if (role == Role.ADMIN || role == Role.SENIAT) {
            existing.setNationalId(null);
            existing.setDistributorId(null);
            existing.setDistributor(null);
            existing.setBranchId(null);
            existing.setBranch(null);
        }

        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }

        User saved = userRepository.save(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            modificationRequestRepository.deleteByRequestedBy_Id(id);
            userRepository.delete(u);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class UserRegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long distributorId;
        private String nationalId;
    }

    @Data
    public static class UserUpdateRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long distributorId;
        private String nationalId;
        private Boolean enabled;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private Role role;
        private Long branchId;
        private Long distributorId;
        private String nationalId;
        private Boolean enabled;

        public UserResponse(Long id, String name, String email, Role role, Long branchId,
                Long distributorId, String nationalId, Boolean enabled) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.branchId = branchId;
            this.distributorId = distributorId;
            this.nationalId = nationalId;
            this.enabled = enabled;
        }
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getUsername(),
                u.getRole(),
                u.getBranchId(),
                u.getDistributorId(),
                u.getNationalId(),
                u.isEnabled());
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

    private String normalizeNationalId(String rawNationalId) {
        if (rawNationalId == null) return null;
        String nationalId = rawNationalId.trim().replaceAll("\\s+", "");
        return nationalId.isBlank() ? null : nationalId;
    }
}
