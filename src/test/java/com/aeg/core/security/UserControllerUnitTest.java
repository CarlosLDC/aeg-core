package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.mail.EmailDeliveryException;
import com.aeg.core.mail.EmailSendResult;
import com.aeg.core.modificationrequest.ModificationRequestRepository;
import com.aeg.core.servicecenter.ServiceCenter;
import com.aeg.core.servicecenter.ServiceCenterRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserControllerUnitTest {

    private UserRepository userRepository;
    private DistributorRepository distributorRepository;
    private BranchRepository branchRepository;
    private ServiceCenterRepository serviceCenterRepository;
    private ModificationRequestRepository modificationRequestRepository;
    private PasswordEncoder passwordEncoder;
    private UserRoleAssignmentService userRoleAssignmentService;
    private UserAccountLifecycleService userAccountLifecycleService;
    private UserActionTokenRepository userActionTokenRepository;
    private UserController controller;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        distributorRepository = mock(DistributorRepository.class);
        branchRepository = mock(BranchRepository.class);
        serviceCenterRepository = mock(ServiceCenterRepository.class);
        modificationRequestRepository = mock(ModificationRequestRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userRoleAssignmentService = new UserRoleAssignmentService(
                distributorRepository,
                branchRepository,
                serviceCenterRepository);
        userAccountLifecycleService = mock(UserAccountLifecycleService.class);
        userActionTokenRepository = mock(UserActionTokenRepository.class);
        controller = new UserController(
                userRepository,
                distributorRepository,
                branchRepository,
                serviceCenterRepository,
                modificationRequestRepository,
                passwordEncoder,
                userRoleAssignmentService,
                userAccountLifecycleService,
                userActionTokenRepository);
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
    void createUser_withDistributorAssignment_derivesDistributorEvenWhenRoleSaysTechnician() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Operativo");
        req.setEmail("dist@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setDistributorId(7L);
        req.setNationalId("V12345678");

        Distributor distributor = distributorWithOrgRole(7L, BranchOrganizationRole.DISTRIBUTOR);

        when(userRepository.findByUsername("dist@aeg.local")).thenReturn(Optional.empty());
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
        assertThat(resp.getBody().getRole()).isEqualTo(Role.DISTRIBUTOR);
        assertThat(resp.getBody().getDistributorId()).isEqualTo(7L);
    }

    @Test
    void createUser_distributor_withDistributorAndNationalId_succeeds() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Distribuidor");
        req.setEmail("dist@aeg.local");
        req.setPassword("p");
        req.setRole("DISTRIBUTOR");
        req.setDistributorId(7L);
        req.setNationalId("V87654321");

        Distributor distributor = distributorWithOrgRole(7L, BranchOrganizationRole.DISTRIBUTOR);

        when(userRepository.findByUsername("dist@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByNationalId("V87654321")).thenReturn(Optional.empty());
        when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getRole()).isEqualTo(Role.DISTRIBUTOR);
    }

    @Test
    void createUser_distributor_withStaleBranchMetadata_succeeds() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Distribuidor");
        req.setEmail("dist-stale@aeg.local");
        req.setPassword("p");
        req.setRole("DISTRIBUTOR");
        req.setDistributorId(7L);
        req.setNationalId("V87654321");

        Branch branch = new Branch();
        branch.setId(99L);
        branch.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);
        branch.setIsDistributor(true);
        branch.setIsServiceCenter(true);
        Distributor distributor = new Distributor();
        distributor.setId(7L);
        distributor.setBranch(branch);

        when(userRepository.findByUsername("dist-stale@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByNationalId("V87654321")).thenReturn(Optional.empty());
        when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getRole()).isEqualTo(Role.DISTRIBUTOR);
        assertThat(resp.getBody().getDistributorId()).isEqualTo(7L);
    }

    @Test
    void createUser_serviceCenterBranch_derivesTechnician() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Centro");
        req.setEmail("sc@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setBranchId(12L);
        req.setNationalId("V99999999");

        Branch branch = serviceCenterBranch(12L);

        ServiceCenter center = new ServiceCenter();
        center.setId(5L);
        center.setBranch(branch);

        when(userRepository.findByUsername("sc@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByNationalId("V99999999")).thenReturn(Optional.empty());
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(serviceCenterRepository.findByBranch_Id(12L)).thenReturn(Optional.of(center));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(4L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(resp.getBody().getBranchId()).isEqualTo(12L);
    }

    @Test
    void createUser_rejectsLegacyServiceCenterRole() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Centro");
        req.setEmail("sc@aeg.local");
        req.setPassword("p");
        req.setRole("SERVICE_CENTER");
        req.setBranchId(12L);
        req.setNationalId("V99999999");

        when(userRepository.findByUsername("sc@aeg.local")).thenReturn(Optional.empty());

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void createUser_technician_withoutNationalId_succeeds() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Técnico");
        req.setEmail("tech@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setBranchId(12L);

        Branch branch = serviceCenterBranch(12L);
        ServiceCenter center = new ServiceCenter();
        center.setBranch(branch);

        when(userRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(serviceCenterRepository.findByBranch_Id(12L)).thenReturn(Optional.of(center));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(resp.getBody().getNationalId()).isNull();
    }

    @Test
    void createUser_technician_withDuplicateNationalId_returnsConflict() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Técnico");
        req.setEmail("tech@aeg.local");
        req.setPassword("p");
        req.setRole("TECHNICIAN");
        req.setBranchId(12L);
        req.setNationalId("V12345678");

        Branch branch = serviceCenterBranch(12L);
        ServiceCenter center = new ServiceCenter();
        center.setBranch(branch);

        when(userRepository.findByUsername("tech@aeg.local")).thenReturn(Optional.empty());
        when(branchRepository.findById(12L)).thenReturn(Optional.of(branch));
        when(serviceCenterRepository.findByBranch_Id(12L)).thenReturn(Optional.of(center));
        when(userRepository.findByNationalId("V12345678")).thenReturn(Optional.of(new User()));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void createUser_asAdminWithoutNationalId_succeeds() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Admin");
        req.setEmail("admin-new@aeg.local");
        req.setPassword("p");
        req.setRole("ADMIN");

        when(userRepository.findByUsername("admin-new@aeg.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("p")).thenReturn("encoded");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(98L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getRole()).isEqualTo(Role.ADMIN);
        assertThat(resp.getBody().getNationalId()).isNull();
    }

    @Test
    void createUser_distributor_withoutNationalId_succeeds() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Distribuidor");
        req.setEmail("dist-noid@aeg.local");
        req.setPassword("p");
        req.setRole("DISTRIBUTOR");
        req.setDistributorId(7L);

        Distributor distributor = distributorWithOrgRole(7L, BranchOrganizationRole.DISTRIBUTOR);

        when(userRepository.findByUsername("dist-noid@aeg.local")).thenReturn(Optional.empty());
        when(distributorRepository.findById(7L)).thenReturn(Optional.of(distributor));
        when(passwordEncoder.encode("p")).thenReturn("enc-p");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(97L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getRole()).isEqualTo(Role.DISTRIBUTOR);
        assertThat(resp.getBody().getNationalId()).isNull();
    }

    @Test
    void createUser_asAdminWithNationalId_persistsProfile() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Admin");
        req.setEmail("admin-new@aeg.local");
        req.setPassword("p");
        req.setRole("ADMIN");
        req.setNationalId("V87654321");

        when(userRepository.findByUsername("admin-new@aeg.local")).thenReturn(Optional.empty());
        when(userRepository.findByNationalId("V87654321")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("p")).thenReturn("encoded");
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getNationalId()).isEqualTo("V87654321");
        assertThat(resp.getBody().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void deleteUser_removesModificationRequestsBeforeDeletingUser() {
        User existing = User.builder()
                .id(3L)
                .username("tech@aeg.local")
                .name("Técnico")
                .password("x")
                .role(Role.TECHNICIAN)
                .branchId(12L)
                .nationalId("V12345678")
                .enabled(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));

        ResponseEntity<Void> resp = controller.deleteUser(3L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userActionTokenRepository).deleteByUser_Id(3L);
        verify(modificationRequestRepository).deleteByRequestedBy_Id(3L);
        verify(userRepository).delete(existing);
    }

    @Test
    void createUser_withSendInvite_createsPendingUserAndSendsEmail() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Invitado");
        req.setEmail("invite@aeg.local");
        req.setRole("ADMIN");
        req.setSendInvite(true);

        when(userRepository.findByUsername("invite@aeg.local")).thenReturn(Optional.empty());
        when(userAccountLifecycleService.createInvitedUserWithToken(ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(50L);
                    u.setEnabled(false);
                    u.setInvitationPending(true);
                    return new UserAccountLifecycleService.IssuedUserToken(u, "raw-token-123");
                });
        when(userAccountLifecycleService.buildInvitationUrl("raw-token-123"))
                .thenReturn("https://aeg-admin.tech/accept-invite?token=raw-token-123");
        when(userAccountLifecycleService.sendInvitationEmail(ArgumentMatchers.any(User.class), eq("raw-token-123")))
                .thenReturn(EmailSendResult.SENT);

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getEnabled()).isFalse();
        assertThat(resp.getBody().getInvitationPending()).isTrue();
        assertThat(resp.getBody().getEmailStatus()).isEqualTo("SENT");
        assertThat(resp.getBody().getInviteUrl()).isEqualTo("https://aeg-admin.tech/accept-invite?token=raw-token-123");
    }

    @Test
    void createUser_withSendInvite_whenEmailFails_returnsCreatedWithFailedStatus() {
        UserController.UserRegistrationRequest req = new UserController.UserRegistrationRequest();
        req.setName("Invitado");
        req.setEmail("invite-fail@aeg.local");
        req.setRole("SENIAT");
        req.setSendInvite(true);

        when(userRepository.findByUsername("invite-fail@aeg.local")).thenReturn(Optional.empty());
        when(userAccountLifecycleService.createInvitedUserWithToken(ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(51L);
                    u.setEnabled(false);
                    u.setInvitationPending(true);
                    return new UserAccountLifecycleService.IssuedUserToken(u, "raw-token-fail");
                });
        when(userAccountLifecycleService.sendInvitationEmail(ArgumentMatchers.any(User.class), eq("raw-token-fail")))
                .thenThrow(new EmailDeliveryException("Resend 500"));

        ResponseEntity<UserController.UserResponse> resp = controller.createUser(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getEmailStatus()).isEqualTo("FAILED");
    }

    @Test
    void resendInvite_whenPending_succeedsAndReturnsStatus() {
        User pending = User.builder()
                .id(10L)
                .username("pending@aeg.local")
                .name("Pendiente")
                .password("x")
                .role(Role.ADMIN)
                .enabled(false)
                .invitationPending(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(userAccountLifecycleService.reissueInvitationToken(pending))
                .thenReturn(new UserAccountLifecycleService.IssuedUserToken(pending, "new-raw"));
        when(userAccountLifecycleService.sendInvitationEmail(pending, "new-raw"))
                .thenReturn(EmailSendResult.LOGGED_ONLY);

        ResponseEntity<UserController.UserResponse> resp = controller.resendInvite(10L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getEmailStatus()).isEqualTo("LOGGED_ONLY");
    }

    @Test
    void resendInvite_whenAlreadyActive_returnsConflict() {
        User active = User.builder()
                .id(11L)
                .username("active@aeg.local")
                .name("Activo")
                .password("x")
                .role(Role.ADMIN)
                .enabled(true)
                .invitationPending(false)
                .build();

        when(userRepository.findById(11L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> controller.resendInvite(11L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void sendPasswordReset_whenActive_succeeds() {
        User active = User.builder()
                .id(12L)
                .username("active@aeg.local")
                .name("Activo")
                .password("x")
                .role(Role.ADMIN)
                .enabled(true)
                .invitationPending(false)
                .build();

        when(userRepository.findById(12L)).thenReturn(Optional.of(active));
        when(userAccountLifecycleService.issuePasswordResetToken(active)).thenReturn("reset-raw");
        when(userAccountLifecycleService.buildPasswordResetUrl("reset-raw"))
                .thenReturn("https://aeg-admin.tech/reset-password?token=reset-raw");
        when(userAccountLifecycleService.sendPasswordResetEmail(active, "reset-raw"))
                .thenReturn(EmailSendResult.SENT);

        ResponseEntity<Map<String, String>> resp = controller.sendPasswordReset(12L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("emailStatus", "SENT");
        assertThat(resp.getBody()).containsEntry("resetUrl", "https://aeg-admin.tech/reset-password?token=reset-raw");
    }

    @Test
    void updateUser_adminToDistributor_persistsDistributorProfile() {
        User existing = User.builder()
                .id(1L)
                .username("admin@aeg.local")
                .name("Admin")
                .password("x")
                .role(Role.ADMIN)
                .branchId(12L)
                .nationalId("V12345678")
                .enabled(true)
                .build();

        UserController.UserUpdateRequest req = new UserController.UserUpdateRequest();
        req.setRole("DISTRIBUTOR");
        req.setDistributorId(7L);
        req.setBranchId(null);
        req.setNationalId("V12345678");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(distributorRepository.findById(7L))
                .thenReturn(Optional.of(distributorWithOrgRole(7L, BranchOrganizationRole.DISTRIBUTOR)));
        when(userRepository.existsByNationalIdAndIdNot("V12345678", 1L)).thenReturn(false);
        when(userRepository.save(ArgumentMatchers.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<UserController.UserResponse> resp = controller.updateUser(1L, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getRole()).isEqualTo(Role.DISTRIBUTOR);
        assertThat(resp.getBody().getDistributorId()).isEqualTo(7L);
        assertThat(resp.getBody().getBranchId()).isNull();
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

    private static Distributor distributorWithOrgRole(Long id, BranchOrganizationRole orgRole) {
        Branch branch = new Branch();
        branch.setId(99L);
        branch.setOrganizationRole(orgRole);
        Distributor distributor = new Distributor();
        distributor.setId(id);
        distributor.setBranch(branch);
        return distributor;
    }

    private static Branch serviceCenterBranch(Long id) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);
        branch.setIsServiceCenter(true);
        return branch;
    }
}
