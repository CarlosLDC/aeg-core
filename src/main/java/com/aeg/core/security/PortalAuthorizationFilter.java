package com.aeg.core.security;

import com.aeg.core.fiscalbookuser.FiscalBookUser;
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

    private final JwtService jwtService;

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

        String portal = resolvePortal(request, authentication);
        if (Portal.FISCAL_BOOK.equals(portal)) {
            if (!isFiscalPortalPath(path)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } else if (isFiscalOnlyAuthPath(path)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/fiscal-book/login")
                || path.startsWith("/actuator/")
                || path.startsWith("/error")
                || path.startsWith("/ws/mqtt");
    }

    private boolean isFiscalPortalPath(String path) {
        return path.startsWith("/api/auth/fiscal-book/")
                || path.startsWith("/api/fiscal-books/");
    }

    private boolean isFiscalOnlyAuthPath(String path) {
        return path.startsWith("/api/auth/fiscal-book/");
    }

    private String resolvePortal(HttpServletRequest request, Authentication authentication) {
        if (authentication.getPrincipal() instanceof FiscalBookUser) {
            return Portal.FISCAL_BOOK;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String portal = jwtService.extractPortal(authHeader.substring(7));
            if (portal != null) {
                return portal;
            }
        }
        return Portal.CORE_ADMIN;
    }
}
