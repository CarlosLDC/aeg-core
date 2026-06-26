package com.aeg.core.company;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.dto.CompanyRequest;
import com.aeg.core.security.SecurityScopeService;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository repository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private SecurityScopeService securityScope;

    private CompanyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyServiceImpl(
                repository,
                branchRepository,
                clientRepository,
                securityScope);
    }

    @Test
    void createRejectsMissingContributorType() {
        CompanyRequest request = new CompanyRequest(
                "Acme C.A.",
                "J123456789",
                null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tipo de contribuyente es obligatorio.");

        verify(repository, never()).save(any());
    }
}
