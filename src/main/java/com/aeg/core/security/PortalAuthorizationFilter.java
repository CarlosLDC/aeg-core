package com.aeg.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PortalAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authentication.getPrincipal() instanceof User user && user.getRole() == Role.SENIAT) {
            if (!isFiscalPortalPath(path) && !isAuthProfilePath(path)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/actuator/")
                || path.startsWith("/error")
                || path.startsWith("/ws/mqtt")
                || path.startsWith("/api/mqtt/enajenacion/stream");
    }

    private boolean isAuthProfilePath(String path) {
        return "/api/auth/me".equals(path);
    }

    private boolean isFiscalPortalPath(String path) {
        return path.startsWith("/api/fiscal-books/")
                || path.startsWith("/api/technical-services")
                || path.startsWith("/api/annual-inspections")
                || path.startsWith("/api/seals")
                || path.startsWith("/api/technicians")
                || path.startsWith("/api/employees")
                || path.startsWith("/api/service-centers")
                || path.startsWith("/api/printers");
    }
}
