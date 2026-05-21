package com.aeg.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserControllerUnitTest {

    private UserRepository userRepository;
    private BranchRepository branchRepository;
    private DistributorRepository distributorRepository;
    private PasswordEncoder passwordEncoder;
    private UserController controller;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        branchRepository = mock(BranchRepository.class);
        distributorRepository = mock(DistributorRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new UserController(userRepository, branchRepository, distributorRepository, passwordEncoder);
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
    void createUser_withBranchId_setsBranchOnResponse() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setUsername("new@aeg.local");
        req.setPassword("p");
        req.setRole("ADMIN");
        req.setBranchId(10L);

        Branch branch = new Branch();
        branch.setId(10L);

        when(userRepository.findByUsername("new@aeg.local")).thenReturn(Optional.empty());
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getBranchId()).isEqualTo(10L);
    }

    @Test
    void createUser_distributorRole_withoutDistributorBranch_returnsBadRequest() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setUsername("dist@aeg.local");
        req.setPassword("p");
        req.setRole("DISTRIBUTOR");
        req.setBranchId(10L);
        req.setDistributorId(5L);

        Branch branch = new Branch();
        branch.setId(10L);

        when(userRepository.findByUsername("dist@aeg.local")).thenReturn(Optional.empty());
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(distributorRepository.findByBranch_Id(10L)).thenReturn(Optional.empty());

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void createUser_distributorRole_withMatchingBranch_setsDistributor() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setUsername("dist@aeg.local");
        req.setPassword("p");
        req.setRole("DISTRIBUTOR");
        req.setBranchId(10L);
        req.setDistributorId(5L);

        Branch branch = new Branch();
        branch.setId(10L);
        Distributor distributor = new Distributor();
        distributor.setId(5L);
        distributor.setBranch(branch);

        when(userRepository.findByUsername("dist@aeg.local")).thenReturn(Optional.empty());
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(distributorRepository.findByBranch_Id(10L)).thenReturn(Optional.of(distributor));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getDistributorId()).isEqualTo(5L);
        assertThat(resp.getBody().getBranchId()).isEqualTo(10L);
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
