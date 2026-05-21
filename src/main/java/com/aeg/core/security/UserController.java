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

import java.util.List;
import java.net.URI;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final com.aeg.core.distributor.DistributorRepository distributorRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegistrationRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
        }

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                .orElse(null);
            if (branch == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }
        }

        Role role = Role.valueOf(request.getRole().toUpperCase());
        DistributorAssignment assignment;
        try {
            assignment = resolveDistributorAssignment(
                    role, request.getBranchId(), request.getDistributorId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        User user = User.builder()
                .username(request.getUsername())
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
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            // Check uniqueness (allow same username if it's the same user)
            var found = userRepository.findByUsername(request.getUsername());
            if (found.isPresent() && !found.get().getId().equals(existing.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
            }
            existing.setUsername(request.getUsername());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        Role role = existing.getRole();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
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
        Long distributorId = existing.getDistributorId();
        if (request.getDistributorId() != null) {
            distributorId = request.getDistributorId();
        }
        try {
            DistributorAssignment assignment = resolveDistributorAssignment(role, branchId, distributorId);
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
            userRepository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class UserRegistrationRequest {
        private String username;
        private String password;
        private String role;
        private Long branchId;
        private Long distributorId;
    }

    @Data
    public static class UserUpdateRequest {
        private String username;
        private String password;
        private String role;
        private Long branchId;
        private Long distributorId;
        private Boolean enabled;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String username;
        private Role role;
        private Long branchId;
        private Long distributorId;
        private Boolean enabled;

        public UserResponse(Long id, String username, Role role, Long branchId, Long distributorId, Boolean enabled) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.branchId = branchId;
            this.distributorId = distributorId;
            this.enabled = enabled;
        }
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.getBranchId(), u.getDistributorId(), u.isEnabled());
    }

    private record DistributorAssignment(Long distributorId, Distributor distributor) {}

    /**
     * Usuario DISTRIBUTOR: sucursal obligatoria con rol distribuidor en catálogo y
     * distributorId alineado a esa sucursal.
     */
    private DistributorAssignment resolveDistributorAssignment(
            Role role, Long branchId, Long distributorId) {
        if (role != Role.DISTRIBUTOR) {
            return new DistributorAssignment(null, null);
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Distributor role requires branch");
        }
        Distributor onBranch = distributorRepository.findByBranch_Id(branchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "branch is not registered as distributor"));
        if (distributorId != null && !distributorId.equals(onBranch.getId())) {
            throw new IllegalArgumentException(
                    "distributorId does not match distributor on branch");
        }
        return new DistributorAssignment(onBranch.getId(), onBranch);
    }
}
