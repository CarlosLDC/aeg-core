package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

class AdminJwtTokenValidatorTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private AdminJwtTokenValidator validator;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "test-secret-key-with-at-least-32-characters!");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        userDetailsService = mock(UserDetailsService.class);
        validator = new AdminJwtTokenValidator(jwtService, userDetailsService);
    }

    @Test
    void rejectsDisabledAdminEvenWithValidUnexpiredJwt() {
        User disabledAdmin = User.builder()
                .username("disabled-admin@test.local")
                .password("hashed")
                .role(Role.ADMIN)
                .enabled(false)
                .build();
        String token = jwtService.generateToken(disabledAdmin);
        when(userDetailsService.loadUserByUsername("disabled-admin@test.local")).thenReturn(disabledAdmin);

        assertThat(validator.validateAdminToken(token)).isEmpty();
    }

    @Test
    void acceptsEnabledAdminWithValidJwt() {
        User activeAdmin = User.builder()
                .username("active-admin@test.local")
                .password("hashed")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        String token = jwtService.generateToken(activeAdmin);
        when(userDetailsService.loadUserByUsername("active-admin@test.local")).thenReturn(activeAdmin);

        assertThat(validator.validateAdminToken(token)).contains("active-admin@test.local");
    }
}
