package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchOrganizationRole;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.company.ContributorType;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.mqtt.MqttConnectionProbeService;
import com.aeg.core.mqtt.MqttService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.mqtt.broker-url=tcp://localhost:1883",
                "app.mqtt.client-id=test-auth-me-client",
                "app.mqtt.default-topic=aeg/test",
                "app.security.admin.name=Test Admin",
                "app.security.admin.username=admin-me@test.local",
                "app.security.admin.password=admin-pass",
                "spring.jpa.open-in-view=false"
        })
class AuthControllerMeIT {

    private static final String PASS = "test-pass";
    private static final String ADMIN_USER = "admin-me@test.local";
    private static final String ADMIN_PASS = "admin-pass";
    private static final String DISTRIBUTOR_USER = "dist-me@test.local";
    private static final String TECHNICIAN_USER = "tech-me@test.local";
    private static final String SENIAT_USER = "seniat-me@test.local";

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    DistributorRepository distributorRepository;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @BeforeEach
    void seedUsers() {
        Company company = companyRepository.findByRif("J-999000112").orElseGet(() -> {
            Company c = new Company();
            c.setBusinessName("Auth Me Test Co");
            c.setRif("J999000112");
            c.setContributorType(ContributorType.ORDINARIO);
            return companyRepository.save(c);
        });

        Branch distributorBranch = branchRepository
                .findFirstByCompany_IdAndCityIgnoreCaseAndStateIgnoreCase(
                        company.getId(), "Caracas", "Distrito Capital")
                .orElseGet(() -> {
                    Branch b = new Branch();
                    b.setCompany(company);
                    b.setCity("Caracas");
                    b.setState("Distrito Capital");
                    b.setOrganizationRole(BranchOrganizationRole.DISTRIBUTOR);
                    b.setIsDistributor(true);
                    return branchRepository.save(b);
                });

        Distributor distributor = distributorRepository.findByBranch_Id(distributorBranch.getId())
                .orElseGet(() -> {
                    Distributor d = new Distributor();
                    d.setBranch(distributorBranch);
                    return distributorRepository.save(d);
                });

        Branch serviceBranch = branchRepository
                .findFirstByCompany_IdAndCityIgnoreCaseAndStateIgnoreCase(
                        company.getId(), "Valencia", "Carabobo")
                .orElseGet(() -> {
                    Branch b = new Branch();
                    b.setCompany(company);
                    b.setCity("Valencia");
                    b.setState("Carabobo");
                    b.setOrganizationRole(BranchOrganizationRole.SERVICE_CENTER);
                    b.setIsServiceCenter(true);
                    return branchRepository.save(b);
                });

        ensureUser(DISTRIBUTOR_USER, Role.DISTRIBUTOR, distributor.getId(), null);
        ensureUser(TECHNICIAN_USER, Role.TECHNICIAN, null, serviceBranch.getId());
        ensureUser(SENIAT_USER, Role.SENIAT, null, null);
    }

    @Test
    void meReturnsProfileWhenOpenInViewDisabledForAllRoles() throws Exception {
        var adminRes = get("/api/auth/me", tokenFor(ADMIN_USER, ADMIN_PASS, "CORE_ADMIN"));
        assertThat(adminRes.statusCode()).isEqualTo(200);
        assertThat(adminRes.body()).contains("\"role\":\"ADMIN\"");

        var distRes = get("/api/auth/me", tokenFor(DISTRIBUTOR_USER, PASS, "CORE_ADMIN"));
        assertThat(distRes.statusCode()).isEqualTo(200);
        assertThat(distRes.body()).contains("\"role\":\"DISTRIBUTOR\"");
        assertThat(distRes.body()).contains("\"canWriteAnnualInspection\":true");

        var techRes = get("/api/auth/me", tokenFor(TECHNICIAN_USER, PASS, "FISCAL_BOOK"));
        assertThat(techRes.statusCode()).isEqualTo(200);
        assertThat(techRes.body()).contains("\"role\":\"TECHNICIAN\"");

        var seniatRes = get("/api/auth/me", tokenFor(SENIAT_USER, PASS, "FISCAL_BOOK"));
        assertThat(seniatRes.statusCode()).isEqualTo(200);
        assertThat(seniatRes.body()).contains("\"role\":\"SENIAT\"");
    }

    private void ensureUser(String username, Role role, Long distributorId, Long branchId) {
        userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
                User.builder()
                        .username(username)
                        .name(username)
                        .password(passwordEncoder.encode(PASS))
                        .role(role)
                        .distributorId(distributorId)
                        .branchId(branchId)
                        .enabled(true)
                        .build()));
    }

    private String tokenFor(String username, String password, String portal) throws Exception {
        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"password\":\"" + password + "\","
                + "\"portal\":\"" + portal + "\""
                + "}";
        var response = post("/api/auth/login", body);
        assertThat(response.statusCode()).isEqualTo(200);
        Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private java.net.http.HttpResponse<String> get(String path, String token) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return java.net.http.HttpClient.newHttpClient().send(
                request,
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private java.net.http.HttpResponse<String> post(String path, String body) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return java.net.http.HttpClient.newHttpClient().send(
                request,
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }
}
