package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.company.ContributorType;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.mqtt.MqttConnectionProbeService;
import com.aeg.core.mqtt.MqttService;
import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.servicecenter.ServiceCenter;
import com.aeg.core.servicecenter.ServiceCenterRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.mqtt.broker-url=tcp://localhost:1883",
                "app.mqtt.client-id=test-role-perms",
                "app.mqtt.default-topic=aeg/test",
                "app.security.admin.name=Test Admin",
                "app.security.admin.username=admin@test.local",
                "app.security.admin.password=admin-pass"
        })
class RolePermissionsIT {

    private static final String PASS = "test-pass";

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
    ClientRepository clientRepository;

    @Autowired
    DistributorRepository distributorRepository;

    @Autowired
    ServiceCenterRepository serviceCenterRepository;

    @Autowired
    PrinterRepository printerRepository;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    Long printerId;
    Long distributorUserId;
    Long technicianUserId;
    Long serviceCenterUserId;
    Long serviceCenterId;

    @BeforeEach
    void seedFixtures() throws Exception {
        Company company = new Company();
        company.setBusinessName("Role Perm Co");
        company.setRif("V55599901");
        company.setContributorType(ContributorType.ORDINARIO);
        company = companyRepository.save(company);

        Branch distributorBranch = new Branch();
        distributorBranch.setCompany(company);
        distributorBranch.setCity("Caracas");
        distributorBranch.setState("Distrito Capital");
        distributorBranch = branchRepository.save(distributorBranch);

        Distributor distributor = new Distributor();
        distributor.setBranch(distributorBranch);
        distributor = distributorRepository.save(distributor);

        Client client = new Client();
        client.setBranch(distributorBranch);
        client.setDistributorId(distributor.getId());
        client = clientRepository.save(client);

        Printer printer = new Printer();
        printer.setFiscalSerial("RP1234567");
        printer.setStatus(PrinterStatus.ASIGNADA);
        printer.setDeviceType(DeviceType.INTERNO);
        printer.setPaid(Boolean.FALSE);
        printer.setClient(client);
        printer.setDistributor(distributor);
        printer = printerRepository.save(printer);
        printerId = printer.getId();

        Branch serviceBranch = new Branch();
        serviceBranch.setCompany(company);
        serviceBranch.setCity("Valencia");
        serviceBranch.setState("Carabobo");
        serviceBranch.setIsServiceCenter(true);
        serviceBranch = branchRepository.save(serviceBranch);

        ServiceCenter serviceCenter = new ServiceCenter();
        serviceCenter.setBranch(serviceBranch);
        serviceCenter = serviceCenterRepository.save(serviceCenter);
        serviceCenterId = serviceCenter.getId();

        User distributorUser = ensureUser(
                "distributor@test.local",
                Role.DISTRIBUTOR,
                distributor.getId(),
                null,
                "V11111111");
        User technicianUser = ensureUser(
                "technician@test.local",
                Role.TECHNICIAN,
                distributor.getId(),
                null,
                "V22222222");
        User serviceCenterUser = ensureUser(
                "service-center@test.local",
                Role.SERVICE_CENTER,
                null,
                serviceBranch.getId(),
                "V33333333");

        distributorUserId = distributorUser.getId();
        technicianUserId = technicianUser.getId();
        serviceCenterUserId = serviceCenterUser.getId();
    }

    @Test
    void distributorCannotCreateTechnicalServiceButCanCreateAnnualInspection() throws Exception {
        String token = tokenFor("distributor@test.local");

        var technicalService = post("/api/technical-services", minimalTechnicalServiceBody(technicianUserId), token);
        assertThat(technicalService.statusCode()).isEqualTo(403);

        var inspection = post(
                "/api/annual-inspections",
                minimalAnnualInspectionBody(distributorUserId),
                token);
        assertThat(inspection.statusCode()).isEqualTo(201);
    }

    @Test
    void technicianCannotCreateTechnicalServiceButCanCreateAnnualInspection() throws Exception {
        String token = tokenFor("technician@test.local");

        var technicalService = post("/api/technical-services", minimalTechnicalServiceBody(technicianUserId), token);
        assertThat(technicalService.statusCode()).isEqualTo(403);

        var inspection = post(
                "/api/annual-inspections",
                minimalAnnualInspectionBody(technicianUserId),
                token);
        assertThat(inspection.statusCode()).isEqualTo(201);
    }

    @Test
    void serviceCenterCanCreateTechnicalServiceWithTechnicianSignerAndAnnualInspection() throws Exception {
        String token = tokenFor("service-center@test.local");

        var technicalService = post(
                "/api/technical-services",
                minimalTechnicalServiceBody(technicianUserId),
                token);
        assertThat(technicalService.statusCode()).isEqualTo(201);
        assertThat(technicalService.body()).contains("\"userId\":" + technicianUserId);

        var inspection = post(
                "/api/annual-inspections",
                minimalAnnualInspectionBody(serviceCenterUserId),
                token);
        assertThat(inspection.statusCode()).isEqualTo(201);
    }

    private User ensureUser(
            String username,
            Role role,
            Long distributorId,
            Long branchId,
            String nationalId) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
                User.builder()
                        .username(username)
                        .name(username)
                        .password(passwordEncoder.encode(PASS))
                        .role(role)
                        .distributorId(distributorId)
                        .branchId(branchId)
                        .nationalId(nationalId)
                        .enabled(true)
                        .build()));
    }

    private String minimalAnnualInspectionBody(Long inspectorUserId) {
        return "{"
                + "\"printerId\":" + printerId + ","
                + "\"userId\":" + inspectorUserId + ","
                + "\"sealTampered\":false,"
                + "\"notes\":null,"
                + "\"photoUrls\":[],"
                + "\"inspectionDate\":\"" + LocalDate.now() + "\""
                + "}";
    }

    private String minimalTechnicalServiceBody(Long signerUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        return "{"
                + "\"printerId\":" + printerId + ","
                + "\"userId\":" + signerUserId + ","
                + "\"serviceCenterId\":" + serviceCenterId + ","
                + "\"sealTampered\":false,"
                + "\"notes\":null,"
                + "\"startAt\":\"" + now + "\","
                + "\"endAt\":\"" + now.plusHours(1) + "\","
                + "\"photoUrls\":[],"
                + "\"initialZReport\":1,"
                + "\"finalZReport\":2,"
                + "\"cost\":" + BigDecimal.ZERO + ","
                + "\"reportedFailure\":\"falla de prueba\","
                + "\"requestDate\":\"" + LocalDate.now() + "\","
                + "\"initialZDate\":\"" + now + "\","
                + "\"finalZDate\":\"" + now.plusHours(1) + "\""
                + "}";
    }

    private String tokenFor(String username) throws Exception {
        var response = login(username, PASS, "FISCAL_BOOK");
        assertThat(response.statusCode()).isEqualTo(200);
        Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private java.net.http.HttpResponse<String> login(String username, String password, String portal)
            throws Exception {
        String body = "{"
                + "\"username\":\"" + username + "\","
                + "\"password\":\"" + password + "\","
                + "\"portal\":\"" + portal + "\""
                + "}";
        return post("/api/auth/login", body, null);
    }

    private java.net.http.HttpResponse<String> post(String path, String body, String token) throws Exception {
        var builder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return java.net.http.HttpClient.newHttpClient().send(
                builder.build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }
}
