package com.aeg.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserController controller;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new UserController(userRepository, passwordEncoder);
    }

    @Test
    void createUser_whenUsernameExists_returnsConflict() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setUsername("existing@aeg.local");
        req.setPassword("p");
        req.setRole("ADMIN");

        when(userRepository.findByUsername("existing@aeg.local")).thenReturn(Optional.of(new User()));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void updateUser_whenChangingToExistingUsername_returnsConflict() {
        User existing = User.builder().id(1L).username("old@aeg.local").password("x").role(Role.ADMIN).enabled(true).build();
        User other = User.builder().id(2L).username("taken@aeg.local").password("x").role(Role.DISTRIBUTOR).enabled(true).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("taken@aeg.local")).thenReturn(Optional.of(other));

        UserController.UserUpdateRequest req = new UserController.UserUpdateRequest();
        req.setUsername("taken@aeg.local");

        ResponseEntity<UserController.UserResponse> resp = controller.updateUser(1L, req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
