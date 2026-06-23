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

import com.aeg.core.mqtt.MqttConnectionProbeService;
import com.aeg.core.mqtt.MqttService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.mqtt.broker-url=tcp://localhost:1883",
                "app.mqtt.client-id=test-auth-client",
                "app.mqtt.default-topic=aeg/test",
                "app.security.admin.name=Test Admin",
                "app.security.admin.username=admin@test.local",
                "app.security.admin.password=admin-pass"
        })
class UnifiedAuthRbacIT {

    private static final String ADMIN_USER = "admin@test.local";
    private static final String ADMIN_PASS = "admin-pass";
    private static final String SENIAT_USER = "seniat@test.local";
    private static final String SENIAT_PASS = "seniat-pass";

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @BeforeEach
    void seedSeniatUser() {
        userRepository.findByUsername(SENIAT_USER).orElseGet(() -> userRepository.save(
                User.builder()
                        .username(SENIAT_USER)
                        .name("Auditor SENIAT")
                        .password(passwordEncoder.encode(SENIAT_PASS))
                        .role(Role.SENIAT)
                        .enabled(true)
                        .build()));
    }

    @Test
    void seniatCanLoginFromPanelPortalWithFiscalBookToken() throws Exception {
        var response = login(SENIAT_USER, SENIAT_PASS, "CORE_ADMIN");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"token\"");
    }

    @Test
    void seniatCanLoginToFiscalBookPortal() throws Exception {
        var response = login(SENIAT_USER, SENIAT_PASS, "FISCAL_BOOK");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"token\"");
    }

    @Test
    void adminCanAccessFiscalBooksAndNotBlockedOnPanel() throws Exception {
        String token = tokenFor(ADMIN_USER, ADMIN_PASS, "CORE_ADMIN");

        var fiscalBooks = get("/api/fiscal-books/search?query=&page=0&pageSize=10", token);
        assertThat(fiscalBooks.statusCode()).isEqualTo(200);

        var adminUsers = get("/api/admin/users", token);
        assertThat(adminUsers.statusCode()).isEqualTo(200);
    }

    @Test
    void seniatCanReadFiscalBooksButNotAdminUsers() throws Exception {
        String token = tokenFor(SENIAT_USER, SENIAT_PASS, "FISCAL_BOOK");

        var fiscalBooks = get("/api/fiscal-books/search?query=&page=0&pageSize=10", token);
        assertThat(fiscalBooks.statusCode()).isEqualTo(200);

        var adminUsers = get("/api/admin/users", token);
        assertThat(adminUsers.statusCode()).isEqualTo(403);
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

    private String tokenFor(String username, String password, String portal) throws Exception {
        var response = login(username, password, portal);
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
        return java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
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
