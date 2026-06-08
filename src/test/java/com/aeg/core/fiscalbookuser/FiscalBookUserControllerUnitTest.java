package com.aeg.core.fiscalbookuser;

import com.aeg.core.security.UserRepository;
import com.aeg.core.employee.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FiscalBookUserControllerUnitTest {

    private FiscalBookUserRepository fiscalBookUserRepository;
    private UserRepository userRepository;
    private EmployeeRepository employeeRepository;
    private PasswordEncoder passwordEncoder;
    private FiscalBookUserController controller;

    @BeforeEach
    void setup() {
        fiscalBookUserRepository = mock(FiscalBookUserRepository.class);
        userRepository = mock(UserRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new FiscalBookUserController(
                fiscalBookUserRepository,
                userRepository,
                employeeRepository,
                passwordEncoder);
    }

    @Test
    void createUser_whenEmailExistsInPanelUsers_returnsConflict() {
        var req = new FiscalBookUserController.UserRegistrationRequest();
        req.setName("Auditor");
        req.setEmail("auditor@aeg.local");
        req.setPassword("secret");
        req.setRole("FISCAL_AUDITOR");

        when(fiscalBookUserRepository.findByUsername("auditor@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("auditor@aeg.local")).thenReturn(Optional.of(new com.aeg.core.security.User()));

        ResponseEntity<FiscalBookUserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(fiscalBookUserRepository, never()).save(any());
    }

    @Test
    void createUser_technicianWithoutEmployee_returnsBadRequest() {
        var req = new FiscalBookUserController.UserRegistrationRequest();
        req.setName("Tecnico");
        req.setEmail("tech@aeg.local");
        req.setPassword("secret");
        req.setRole("FISCAL_TECHNICIAN");

        when(fiscalBookUserRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());

        ResponseEntity<FiscalBookUserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createUser_auditor_succeeds() {
        var req = new FiscalBookUserController.UserRegistrationRequest();
        req.setName("Auditor");
        req.setEmail("auditor@aeg.local");
        req.setPassword("secret");
        req.setRole("FISCAL_AUDITOR");

        when(fiscalBookUserRepository.findByUsername("auditor@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("auditor@aeg.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        when(fiscalBookUserRepository.save(any())).thenAnswer(invocation -> {
            FiscalBookUser user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        ResponseEntity<FiscalBookUserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getRole()).isEqualTo(FiscalBookRole.FISCAL_AUDITOR);
    }
}
