package com.aeg.core.software;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.software.dto.SoftwareRequest;
import com.aeg.core.software.dto.SoftwareResponse;

class SoftwareServiceImplTest {

    private SoftwareRepository repository;
    private CompanyRepository companyRepository;
    private SoftwareServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SoftwareRepository.class);
        companyRepository = mock(CompanyRepository.class);
        service = new SoftwareServiceImpl(repository, companyRepository);
    }

    @Test
    void createSoftwareWithDeveloperAndDistributors() {
        Company distCompany = new Company();
        distCompany.setId(20L);
        distCompany.setRif("J-987654321");
        distCompany.setBusinessName("Distributor C.A.");

        when(companyRepository.findAllById(List.of(20L))).thenReturn(List.of(distCompany));
        when(repository.save(any(Software.class))).thenAnswer(invocation -> {
            Software s = invocation.getArgument(0);
            s.setId(1L);
            s.setCreatedAt(OffsetDateTime.now());
            return s;
        });

        SoftwareRequest request = new SoftwareRequest(
                "Saint Enterprise",
                "1.0",
                List.of("C#"),
                List.of("Windows"),
                "Software Developer C.A.",
                "J123456789",
                true,
                List.of(20L)
        );

        SoftwareResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Saint Enterprise");
        assertThat(response.version()).isEqualTo("1.0");
        assertThat(response.external()).isTrue();
        assertThat(response.developerRif()).isEqualTo("J-123456789");
        assertThat(response.developerName()).isEqualTo("Software Developer C.A.");
        assertThat(response.distributors()).hasSize(1);
        assertThat(response.distributors().get(0).id()).isEqualTo(20L);
        assertThat(response.distributors().get(0).rif()).isEqualTo("J-987654321");
    }

    @Test
    void addDistributorSuccessfully() {
        Software software = new Software();
        software.setId(1L);
        software.setName("Saint");
        software.setVersion("2.0");
        software.setDistributors(new ArrayList<>());

        Company company = new Company();
        company.setId(30L);
        company.setRif("J-33333333-3");
        company.setBusinessName("Partner S.A.");

        when(repository.findById(1L)).thenReturn(Optional.of(software));
        when(companyRepository.findById(30L)).thenReturn(Optional.of(company));
        when(repository.save(any(Software.class))).thenAnswer(inv -> inv.getArgument(0));

        SoftwareResponse res = service.addDistributor(1L, 30L);

        assertThat(res.distributors()).hasSize(1);
        assertThat(res.distributors().get(0).id()).isEqualTo(30L);
    }

    @Test
    void removeDistributorSuccessfully() {
        Software software = new Software();
        software.setId(1L);
        software.setName("Saint");
        software.setVersion("2.0");

        Company company = new Company();
        company.setId(30L);
        company.setRif("J-33333333-3");
        company.setBusinessName("Partner S.A.");
        software.setDistributors(new ArrayList<>(List.of(company)));

        when(repository.findById(1L)).thenReturn(Optional.of(software));
        when(repository.save(any(Software.class))).thenAnswer(inv -> inv.getArgument(0));

        SoftwareResponse res = service.removeDistributor(1L, 30L);

        assertThat(res.distributors()).isEmpty();
    }
}
