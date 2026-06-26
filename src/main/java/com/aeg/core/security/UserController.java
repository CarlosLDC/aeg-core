package com.aeg.core.security;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
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
    private final DistributorRepository distributorRepository;
    private final BranchRepository branchRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ModificationRequestRepository modificationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleAssignmentService userRoleAssignmentService;

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

        UserRoleAssignmentService.RoleResolution roleResolution =
                userRoleAssignmentService.resolveForCreate(request);
        if (roleResolution.hasError()) {
            return ResponseEntity.status(roleResolution.errorStatus()).build();
        }
        Role role = roleResolution.role();

        ProfileAssignment profile = resolveProfileAssignment(role, request, null);
        if (profile.errorStatus() != null) {
            return ResponseEntity.status(profile.errorStatus()).build();
        }

        User user = User.builder()
                .username(email)
                .name(name)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .branchId(profile.branchId())
                .branch(profile.branch())
                .distributorId(profile.distributorId())
                .distributor(profile.distributor())
                .nationalId(profile.nationalId())
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
        UserRoleAssignmentService.RoleResolution roleResolution =
                userRoleAssignmentService.resolveForUpdate(request, existing);
        if (roleResolution.hasError()) {
            return ResponseEntity.status(roleResolution.errorStatus()).build();
        }
        Role role = roleResolution.role();
        existing.setRole(role);

        ProfileAssignment profile = resolveProfileAssignment(role, request, existing);
        if (profile.errorStatus() != null) {
            return ResponseEntity.status(profile.errorStatus()).build();
        }

        existing.setNationalId(profile.nationalId());
        existing.setDistributorId(profile.distributorId());
        existing.setDistributor(profile.distributor());
        existing.setBranchId(profile.branchId());
        existing.setBranch(profile.branch());

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

    private ProfileAssignment resolveProfileAssignment(
            Role role,
            UserRegistrationRequest createRequest,
            User existing) {
        UserUpdateRequest updateRequest = existing != null ? toUpdateRequest(createRequest, existing) : null;
        return resolveProfileAssignment(role, createRequest, updateRequest, existing);
    }

    private ProfileAssignment resolveProfileAssignment(
            Role role,
            UserUpdateRequest updateRequest,
            User existing) {
        return resolveProfileAssignment(role, null, updateRequest, existing);
    }

    private ProfileAssignment resolveProfileAssignment(
            Role role,
            UserRegistrationRequest createRequest,
            UserUpdateRequest updateRequest,
            User existing) {
        if (role == Role.ADMIN || role == Role.SENIAT) {
            return ProfileAssignment.global();
        }
        if (Role.isDistributorScoped(role)) {
            String nationalId = updateRequest != null && updateRequest.getNationalId() != null
                    ? normalizeNationalId(updateRequest.getNationalId())
                    : createRequest != null
                            ? normalizeNationalId(createRequest.getNationalId())
                            : existing != null ? existing.getNationalId() : null;
            Long distributorId = updateRequest != null && updateRequest.getDistributorId() != null
                    ? updateRequest.getDistributorId()
                    : createRequest != null
                            ? createRequest.getDistributorId()
                            : existing != null ? existing.getDistributorId() : null;
            if (nationalId == null || distributorId == null) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.BAD_REQUEST);
            }
            Long excludeId = existing != null ? existing.getId() : null;
            if (excludeId != null
                    ? userRepository.existsByNationalIdAndIdNot(nationalId, excludeId)
                    : userRepository.findByNationalId(nationalId).isPresent()) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.CONFLICT);
            }
            if (distributorRepository.findById(distributorId).isEmpty()) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.NOT_FOUND);
            }
            Distributor distributor = distributorRepository.findById(distributorId).orElse(null);
            return new ProfileAssignment(nationalId, distributorId, distributor, null, null, null);
        }
        if (role == Role.TECHNICIAN) {
            Long branchId = updateRequest != null && updateRequest.getBranchId() != null
                    ? updateRequest.getBranchId()
                    : createRequest != null
                            ? createRequest.getBranchId()
                            : existing != null ? existing.getBranchId() : null;
            if (branchId == null) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.BAD_REQUEST);
            }
            Branch branch = branchRepository.findById(branchId).orElse(null);
            if (branch == null || branch.getOrganizationRole() != BranchOrganizationRole.SERVICE_CENTER) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.BAD_REQUEST);
            }
            if (serviceCenterRepository.findByBranch_Id(branchId).isEmpty()) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.BAD_REQUEST);
            }
            String nationalId = updateRequest != null && updateRequest.getNationalId() != null
                    ? normalizeNationalId(updateRequest.getNationalId())
                    : createRequest != null
                            ? normalizeNationalId(createRequest.getNationalId())
                            : existing != null ? existing.getNationalId() : null;
            if (nationalId == null) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.BAD_REQUEST);
            }
            Long excludeId = existing != null ? existing.getId() : null;
            if (excludeId != null
                    ? userRepository.existsByNationalIdAndIdNot(nationalId, excludeId)
                    : userRepository.findByNationalId(nationalId).isPresent()) {
                return ProfileAssignment.error(org.springframework.http.HttpStatus.CONFLICT);
            }
            return new ProfileAssignment(nationalId, null, null, branchId, branch, null);
        }
        return ProfileAssignment.error(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    private UserUpdateRequest toUpdateRequest(UserRegistrationRequest createRequest, User existing) {
        UserUpdateRequest update = new UserUpdateRequest();
        update.setNationalId(createRequest.getNationalId());
        update.setDistributorId(createRequest.getDistributorId());
        update.setBranchId(createRequest.getBranchId());
        if (createRequest.getNationalId() == null) {
            update.setNationalId(existing.getNationalId());
        }
        if (createRequest.getDistributorId() == null) {
            update.setDistributorId(existing.getDistributorId());
        }
        if (createRequest.getBranchId() == null) {
            update.setBranchId(existing.getBranchId());
        }
        return update;
    }

    private record ProfileAssignment(
            String nationalId,
            Long distributorId,
            Distributor distributor,
            Long branchId,
            Branch branch,
            org.springframework.http.HttpStatus errorStatus) {
        static ProfileAssignment global() {
            return new ProfileAssignment(null, null, null, null, null, null);
        }

        static ProfileAssignment error(org.springframework.http.HttpStatus status) {
            return new ProfileAssignment(null, null, null, null, null, status);
        }
    }

    @Data
    public static class UserRegistrationRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long distributorId;
        private Long branchId;
        private String nationalId;
    }

    @Data
    public static class UserUpdateRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long distributorId;
        private Long branchId;
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
