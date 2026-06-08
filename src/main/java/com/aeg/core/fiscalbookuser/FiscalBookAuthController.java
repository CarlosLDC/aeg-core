package com.aeg.core.fiscalbookuser;

import com.aeg.core.security.JwtService;
import com.aeg.core.security.Portal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/fiscal-book")
@RequiredArgsConstructor
public class FiscalBookAuthController {

    private final FiscalBookUserRepository fiscalBookUserRepository;
    private final FiscalBookUserScopeService scopeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        String username = request.getUsername().trim().toLowerCase();
        FiscalBookUser user = fiscalBookUserRepository.findByUsernameWithRelations(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("portal", Portal.FISCAL_BOOK);
        claims.put("role", user.getRole().name());
        claims.put("employeeId", user.getEmployeeId());
        claims.put("distributorId", scopeService.resolveDistributorId(user.getEmployeeId()));

        String token = jwtService.generateToken(claims, user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (!(userDetails instanceof FiscalBookUser user)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getUsername(),
                user.getRole(),
                user.getEmployeeId(),
                scopeService.resolveDistributorId(user.getEmployeeId()),
                user.isEnabled()
        ));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
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
        private final FiscalBookRole role;
        private final Long employeeId;
        private final Long distributorId;
        private final Boolean enabled;
    }
}
