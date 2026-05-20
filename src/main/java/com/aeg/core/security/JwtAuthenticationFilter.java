package com.aeg.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.security.basic-auth.enabled:false}")
    private boolean basicAuthEnabled;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        try {
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    jwt = authHeader.substring(7);
                    userEmail = jwtService.extractUsername(jwt);
                    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                            authToken.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                } else if (basicAuthEnabled && authHeader.startsWith("Basic ")) {
                    // Basic auth solo en tests (app.security.basic-auth.enabled=true)
                    String base64Credentials = authHeader.substring(6);
                    String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                    int idx = credentials.indexOf(':');
                    if (idx > 0) {
                        String username = credentials.substring(0, idx);
                        String password = credentials.substring(idx + 1);
                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                            if (userDetails != null && passwordEncoder.matches(password, userDetails.getPassword())) {
                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Authentication processing failed: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
