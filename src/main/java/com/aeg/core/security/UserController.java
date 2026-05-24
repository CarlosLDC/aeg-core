package com.aeg.core.security;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.servicecenter.ServiceCenterRepository;

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
    private final BranchRepository branchRepository;
    private final com.aeg.core.distributor.DistributorRepository distributorRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ModificationRequestRepository modificationRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegistrationRequest request) {
        String name = normalizeName(request.getName());
        String email = normalizeEmail(request.getEmail());
        if (name == null || email == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getBranchId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (userRepository.findByUsername(email).isPresent()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
        }
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElse(null);
        if (branch == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        RoleAssignment assignment;
        try {
            assignment = resolveRoleAssignment(role, request.getBranchId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        User user = User.builder()
                .username(email)
                .name(name)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .branchId(request.getBranchId())
                .branch(branch)
                .distributorId(assignment.distributorId())
                .distributor(assignment.distributor())
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
            // Check uniqueness (allow same username if it's the same user)
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
        Long branchId = existing.getBranchId();
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElse(null);
            if (branch == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }
            existing.setBranch(branch);
            branchId = branch.getId();
        }
        try {
            RoleAssignment assignment = resolveRoleAssignment(role, branchId);
            existing.setDistributorId(assignment.distributorId());
            existing.setDistributor(assignment.distributor());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }

        User saved = userRepository.save(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
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
        private Long branchId;
    }

    @Data
    public static class UserUpdateRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long branchId;
        private Boolean enabled;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private Role role;
        private Long branchId;
        private Boolean enabled;

        public UserResponse(Long id, String name, String email, Role role, Long branchId, Boolean enabled) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.branchId = branchId;
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
                u.isEnabled());
    }

    private record RoleAssignment(Long distributorId, Distributor distributor) {}

    /**
     * Reglas de elegibilidad por sucursal:
     * - DISTRIBUTOR: la sucursal debe estar registrada como distribuidora.
     * - TECHNICIAN/SERVICE_CENTER: la sucursal debe estar registrada como centro de servicio.
     */
    private RoleAssignment resolveRoleAssignment(Role role, Long branchId) {
        if (role == Role.ADMIN) {
            return new RoleAssignment(null, null);
        }
        if (role != Role.DISTRIBUTOR) {
            if (branchId == null) {
                throw new IllegalArgumentException("branch is required");
            }
            if (role == Role.TECHNICIAN || role == Role.SERVICE_CENTER) {
                boolean hasServiceCenter = serviceCenterRepository.findByBranch_Id(branchId).isPresent();
                if (!hasServiceCenter) {
                    throw new IllegalArgumentException("branch is not registered as service center");
                }
            }
            return new RoleAssignment(null, null);
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Distributor role requires branch");
        }
        Distributor onBranch = distributorRepository.findByBranch_Id(branchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "branch is not registered as distributor"));
        return new RoleAssignment(onBranch.getId(), onBranch);
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
