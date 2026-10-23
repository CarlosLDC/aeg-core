package com.aeg.core.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminJwtTokenValidator {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public Optional<String> validateAdminToken(String token) {
        return validateTokenForRoles(token, "ROLE_ADMIN");
    }

    public Optional<String> validateAdminOrDistributorToken(String token) {
        return validateTokenForRoles(token, "ROLE_ADMIN", "ROLE_DISTRIBUTOR");
    }

    private Optional<String> validateTokenForRoles(String token, String... allowedAuthorities) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails)) {
                return Optional.empty();
            }
            if (!hasAnyAuthority(userDetails, allowedAuthorities)) {
                return Optional.empty();
            }
            return Optional.of(username);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static String extractTokenFromQueryOrHeader(String queryToken, List<String> authorizationHeaders) {
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }
        if (authorizationHeaders != null) {
            for (String header : authorizationHeaders) {
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring(7);
                }
            }
        }
        return null;
    }

    private static boolean hasAnyAuthority(UserDetails userDetails, String... allowedAuthorities) {
        List<String> allowed = List.of(allowedAuthorities);
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowed::contains);
    }
}
