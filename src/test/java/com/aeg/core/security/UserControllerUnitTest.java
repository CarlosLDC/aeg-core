package com.aeg.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    private UserRepository userRepository;
    private DistributorRepository distributorRepository;
    private ModificationRequestRepository modificationRequestRepository;
    private PasswordEncoder passwordEncoder;
    private UserController controller;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        distributorRepository = mock(DistributorRepository.class);
        modificationRequestRepository = mock(ModificationRequestRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new UserController(
                userRepository,
                distributorRepository,
                modificationRequestRepository,
                passwordEncoder);
    }

    @Test
    void createUser_whenEmailExists_returnsConflict() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Usuario Existente");
        req.setEmail("existing@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setDistributorId(7L);
        req.setNationalId("V12345678");

        when(userRepository.findByUsername("existing@aeg.local")).thenReturn(Optional.of(new User()));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void createUser_technician_withDistributorAndNationalId_succeeds() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Técnico");
        req.setEmail("tech@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setDistributorId(7L);
        req.setNationalId("V12345678");

        Distributor distributor = new Distributor();
        distributor.setId(7L);

        when(userRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByNationalId("V12345678")).thenReturn(Optional.empty());
        when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getDistributorId()).isEqualTo(7L);
        assertThat(resp.getBody().getNationalId()).isEqualTo("V12345678");
        assertThat(resp.getBody().getRole()).isEqualTo(Role.TECHNICIAN);
    }

    @Test
    void createUser_technician_withoutNationalId_returnsBadRequest() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Técnico");
        req.setEmail("tech@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setDistributorId(7L);

        when(userRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void createUser_technician_withDuplicateNationalId_returnsConflict() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Técnico");
        req.setEmail("tech@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setDistributorId(7L);
        req.setNationalId("V12345678");

        when(userRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByNationalId("V12345678")).thenReturn(Optional.of(new User()));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void deleteUser_removesModificationRequestsBeforeDeletingUser() {
        User existing = User.builder()
                .id(3L)
                .username("tech@aeg.local")
                .name("Técnico")
                .password("x")
                .role(Role.TECHNICIAN)
                .distributorId(7L)
                .nationalId("V12345678")
                .enabled(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));

        ResponseEntity<Void> resp = controller.deleteUser(3L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(modificationRequestRepository).deleteByRequestedBy_Id(3L);
        verify(userRepository).delete(existing);
    }

    @Test
    void updateUser_whenChangingToExistingEmail_returnsConflict() {
        User existing = User.builder().id(1L).username("old@aeg.local").name("Old").password("x").role(Role.ADMIN).enabled(true).build();
        User other = User.builder().id(2L).username("taken@aeg.local").name("Taken").password("x").role(Role.TECHNICIAN).enabled(true).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("taken@aeg.local")).thenReturn(Optional.of(other));

        UserController.UserUpdateRequest req = new UserController.UserUpdateRequest();
        req.setEmail("taken@aeg.local");

        ResponseEntity<UserController.UserResponse> resp = controller.updateUser(1L, req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
