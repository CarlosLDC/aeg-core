package com.aeg.core.security;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.organization.OrgCapability;
import com.aeg.core.organization.OrganizationCapabilityService;

import java.time.OffsetDateTime;
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
    private final OrganizationCapabilityService organizationCapability;
    private final UserAccountLifecycleService userAccountLifecycleService;
    private final AuthRateLimiter authRateLimiter;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        User user = userRepository.findByUsernameWithRelations(request.getUsername()).orElseThrow();
        String token = issueTokenForUser(user, request.getPortal());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/invitations/validate")
    public ResponseEntity<InvitationValidationResponse> validateInvitation(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.checkTokenEndpoint(extractClientIp(httpRequest));
        UserAccountLifecycleService.InvitationDetails details = userAccountLifecycleService.validateInvitation(token);
        return ResponseEntity.ok(new InvitationValidationResponse(
                true,
                details.name(),
                details.email(),
                details.role(),
                details.expiresAt()
        ));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<AcceptInviteResponse> acceptInvite(
            @RequestBody AcceptInviteRequest request,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.checkTokenEndpoint(extractClientIp(httpRequest));
        User user = userAccountLifecycleService.acceptInvitation(request.getToken(), request.getPassword());
        String token = issueTokenForUser(user, request.getPortal());
        return ResponseEntity.ok(new AcceptInviteResponse(
                token,
                user.getRole(),
                user.getUsername(),
                user.getName()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        userAccountLifecycleService.requestPasswordReset(request.getEmail(), extractClientIp(httpRequest));
        return ResponseEntity.ok(new MessageResponse(
                "Si el correo está asociado a una cuenta activa, recibirás instrucciones para restablecer tu contraseña."
        ));
    }

    @GetMapping("/password-reset/validate")
    public ResponseEntity<PasswordResetValidationResponse> validatePasswordReset(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.checkTokenEndpoint(extractClientIp(httpRequest));
        UserAccountLifecycleService.PasswordResetDetails details =
                userAccountLifecycleService.validatePasswordReset(token);
        return ResponseEntity.ok(new PasswordResetValidationResponse(
                true,
                details.email(),
                details.expiresAt()
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.checkTokenEndpoint(extractClientIp(httpRequest));
        userAccountLifecycleService.resetPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(new MessageResponse(
                "Contraseña actualizada correctamente. Ya puedes iniciar sesión."
        ));
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
            user.isEnabled(),
            organizationCapability.actorHasCapability(OrgCapability.WRITE_ANNUAL_INSPECTION)
        ));
    }

    private String issueTokenForUser(User user, String requestedPortal) {
        String loginPortal = normalizeLoginPortal(requestedPortal);
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

        return jwtService.generateToken(extraClaims, user);
    }

    private static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            if (!first.isBlank()) {
                return first.trim();
            }
        }
        return request.getRemoteAddr();
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
    public static class AcceptInviteRequest {
        private String token;
        private String password;
        private String portal;
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        private String token;
        private String password;
    }

    @Data
    @RequiredArgsConstructor
    public static class AuthResponse {
        private final String token;
    }

    @Data
    @RequiredArgsConstructor
    public static class AcceptInviteResponse {
        private final String token;
        private final Role role;
        private final String email;
        private final String name;
    }

    @Data
    @RequiredArgsConstructor
    public static class InvitationValidationResponse {
        private final boolean valid;
        private final String name;
        private final String email;
        private final Role role;
        private final OffsetDateTime expiresAt;
    }

    @Data
    @RequiredArgsConstructor
    public static class PasswordResetValidationResponse {
        private final boolean valid;
        private final String email;
        private final OffsetDateTime expiresAt;
    }

    @Data
    @RequiredArgsConstructor
    public static class MessageResponse {
        private final String message;
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
        /** Capacidad efectiva del actor para crear/editar inspecciones anuales. */
        private final Boolean canWriteAnnualInspection;
    }
}
