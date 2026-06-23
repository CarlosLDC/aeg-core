package com.aeg.core.security;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsernameWithRelations(request.getUsername()).orElseThrow();

        String loginPortal = normalizeLoginPortal(request.getPortal());
        if (user.getRole() == Role.SENIAT) {
            loginPortal = Portal.FISCAL_BOOK;
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("portal", loginPortal);
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("branchId", user.getBranchId());
        extraClaims.put("distributorId", user.getDistributorId());
        extraClaims.put("nationalId", user.getNationalId());
        extraClaims.put("userId", user.getId());

        String token = jwtService.generateToken(extraClaims, userDetails);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (!(userDetails instanceof User)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User user = userRepository.findByUsernameWithRelations(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new UserProfileResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getUsername(),
            user.getRole(),
            user.getBranchId(),
            user.getDistributorId(),
            user.getNationalId(),
            user.isEnabled()
        ));
    }

    private static String normalizeLoginPortal(String portal) {
        if (portal == null || portal.isBlank()) {
            return Portal.CORE_ADMIN;
        }
        return portal.trim().toUpperCase();
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        /** {@code CORE_ADMIN} (panel) o {@code FISCAL_BOOK} (libro fiscal). */
        private String portal;
    }

    @Data
    @RequiredArgsConstructor
    public static class AuthResponse {
        private final String token;
    }

    @Data
    @RequiredArgsConstructor
    public static class UserProfileResponse {
        private final Long id;
        private final String name;
        private final String email;
        private final String username;
        private final Role role;
        private final Long branchId;
        private final Long distributorId;
        private final String nationalId;
        private final Boolean enabled;
    }
}
