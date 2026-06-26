package com.aeg.core.branch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.branch.dto.BranchRequest;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.security.Role;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.security.UserRepository;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock
    private BranchRepository repository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityScopeService securityScope;

    private BranchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BranchServiceImpl(
                repository,
                companyRepository,
                clientRepository,
                userRepository,
                securityScope);
    }

    @Test
    void createRejectsMissingAddress() {
        BranchRequest request = new BranchRequest(
                1L,
                "Caracas",
                "Distrito Capital",
                "  ",
                null,
                null,
                null,
                false,
                false,
                false,
                BranchOrganizationRole.NONE);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La dirección es obligatoria.");

        verify(repository, never()).save(any());
    }

    @Test
    void updateAllowsMissingAddress() {
        Company company = new Company();
        company.setId(1L);
        Branch branch = new Branch();
        branch.setId(10L);
        branch.setCompany(company);
        branch.setCity("Caracas");
        branch.setState("Distrito Capital");
        branch.setAddress(null);

        when(repository.findById(10L)).thenReturn(Optional.of(branch));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(repository.save(branch)).thenReturn(branch);
        User admin = new User();
        admin.setRole(Role.ADMIN);
        when(securityScope.currentUser()).thenReturn(admin);

        BranchRequest request = new BranchRequest(
                1L,
                "Caracas",
                "Distrito Capital",
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                BranchOrganizationRole.NONE);

        service.update(10L, request);

        verify(repository).save(branch);
    }
}
