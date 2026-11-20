package com.aeg.core.security;

import com.aeg.core.mail.EmailMessage;
import com.aeg.core.mail.EmailSendResult;
import com.aeg.core.mail.EmailSender;
import com.aeg.core.mqtt.MqttConnectionProbeService;
import com.aeg.core.mqtt.MqttService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.mqtt.broker-url=tcp://localhost:1883",
                "app.mqtt.client-id=test-invite-client",
                "app.mqtt.default-topic=aeg/test",
                "app.security.admin.name=Test Admin",
                "app.security.admin.username=admin-invite@test.local",
                "app.security.admin.password=admin-pass",
                "app.mail.async-password-reset=false"
        })
class UserInvitationAndPasswordResetIT {

    private static final String ADMIN_USER = "admin-invite@test.local";
    private static final String ADMIN_PASS = "admin-pass";
    private static final Pattern TOKEN_LINK_PATTERN = Pattern.compile("token=([A-Za-z0-9_\\-%.]+)");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserActionTokenRepository tokenRepository;

    @Autowired
    AuthRateLimiter authRateLimiter;

    @MockitoBean
    MqttService mqttService;

    @MockitoBean
    MqttConnectionProbeService mqttConnectionProbeService;

    @MockitoBean
    EmailSender emailSender;

    private final List<EmailMessage> sentEmails = new CopyOnWriteArrayList<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        sentEmails.clear();
        authRateLimiter.reset();
        when(emailSender.send(any(EmailMessage.class))).thenAnswer(invocation -> {
            EmailMessage msg = invocation.getArgument(0);
            sentEmails.add(msg);
            return EmailSendResult.SENT;
        });
    }

    @Test
    void fullInvitationAndPasswordResetLifecycle() throws Exception {
        String invitedEmail = "invited-lifecycle@test.local";
        userRepository.findByUsername(invitedEmail).ifPresent(existing -> {
            tokenRepository.deleteAll();
            userRepository.delete(existing);
        });

        String adminToken = loginForToken(ADMIN_USER, ADMIN_PASS);

        // 1. Admin creates user with sendInvite=true
        String createBody = """
                {
                  "name": "María Invitada",
                  "email": "%s",
                  "role": "ADMIN",
                  "sendInvite": true
                }
                """.formatted(invitedEmail);
        HttpResponse<String> createResp = postJson("/api/admin/users", createBody, adminToken);
        assertThat(createResp.statusCode()).isEqualTo(201);
        JsonNode createdJson = mapper.readTree(createResp.body());
        long userId = createdJson.get("id").asLong();
        assertThat(createdJson.get("enabled").asBoolean()).isFalse();
        assertThat(createdJson.get("invitationPending").asBoolean()).isTrue();
        assertThat(createdJson.get("emailStatus").asText()).isEqualTo("SENT");
        assertThat(createdJson.get("inviteUrl").asText()).contains("/accept-invite?token=");
        assertThat(sentEmails).hasSize(1);
        String firstToken = extractToken(sentEmails.get(0));

        // 2. Login before activation fails
        HttpResponse<String> preActivateLogin = login(invitedEmail, "any-password");
        assertThat(preActivateLogin.statusCode()).isIn(401, 403);

        // 3. Validate first invitation token
        HttpResponse<String> validateFirst = get("/api/auth/invitations/validate?token=" + firstToken, null);
        assertThat(validateFirst.statusCode()).isEqualTo(200);
        JsonNode validateJson = mapper.readTree(validateFirst.body());
        assertThat(validateJson.get("valid").asBoolean()).isTrue();
        assertThat(validateJson.get("email").asText()).isEqualTo(invitedEmail);
        assertThat(validateJson.get("name").asText()).isEqualTo("María Invitada");
        assertThat(validateJson.get("role").asText()).isEqualTo("ADMIN");

        // 4. Resend invite invalidates first token and issues second token
        HttpResponse<String> resendResp = postJson("/api/admin/users/" + userId + "/resend-invite", "{}", adminToken);
        assertThat(resendResp.statusCode()).isEqualTo(200);
        assertThat(sentEmails).hasSize(2);
        String secondToken = extractToken(sentEmails.get(1));
        assertThat(secondToken).isNotEqualTo(firstToken);

        assertThat(get("/api/auth/invitations/validate?token=" + firstToken, null).statusCode()).isEqualTo(400);
        assertThat(get("/api/auth/invitations/validate?token=" + secondToken, null).statusCode()).isEqualTo(200);

        // 5. Short password fails; valid 8+ char password activates user and returns working JWT
        HttpResponse<String> weakAccept = postJson(
                "/api/auth/accept-invite",
                """
                {"token":"%s","password":"short"}
                """.formatted(secondToken),
                null
        );
        assertThat(weakAccept.statusCode()).isEqualTo(400);

        HttpResponse<String> acceptResp = postJson(
                "/api/auth/accept-invite",
                """
                {"token":"%s","password":"ValidPassword123"}
                """.formatted(secondToken),
                null
        );
        assertThat(acceptResp.statusCode()).isEqualTo(200);
        JsonNode acceptJson = mapper.readTree(acceptResp.body());
        String userJwt = acceptJson.get("token").asText();
        assertThat(userJwt).isNotBlank();
        assertThat(acceptJson.get("role").asText()).isEqualTo("ADMIN");

        HttpResponse<String> meResp = get("/api/auth/me", userJwt);
        assertThat(meResp.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(meResp.body()).get("email").asText()).isEqualTo(invitedEmail);

        // 6. Reusing accepted token returns 400 and resend-invite now returns 409
        assertThat(postJson(
                "/api/auth/accept-invite",
                """
                {"token":"%s","password":"AnotherPassword123"}
                """.formatted(secondToken),
                null
        ).statusCode()).isEqualTo(400);
        assertThat(postJson("/api/admin/users/" + userId + "/resend-invite", "{}", adminToken).statusCode())
                .isEqualTo(409);

        // 7. Forgot password for unknown email returns 200 and sends NO email
        int emailsBeforeUnknown = sentEmails.size();
        HttpResponse<String> unknownForgot = postJson(
                "/api/auth/forgot-password",
                """
                {"email":"nobody@test.local"}
                """,
                null
        );
        assertThat(unknownForgot.statusCode()).isEqualTo(200);
        assertThat(sentEmails).hasSize(emailsBeforeUnknown);

        // Forgot password for active user sends reset email
        HttpResponse<String> forgotResp = postJson(
                "/api/auth/forgot-password",
                """
                {"email":"%s"}
                """.formatted(invitedEmail),
                null
        );
        assertThat(forgotResp.statusCode()).isEqualTo(200);
        assertThat(sentEmails).hasSize(3);
        String resetToken = extractToken(sentEmails.get(2));

        // 8. Validate and reset password
        HttpResponse<String> validateReset = get("/api/auth/password-reset/validate?token=" + resetToken, null);
        assertThat(validateReset.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(validateReset.body()).get("email").asText()).isEqualTo(invitedEmail);

        HttpResponse<String> resetResp = postJson(
                "/api/auth/reset-password",
                """
                {"token":"%s","password":"NewPassword456"}
                """.formatted(resetToken),
                null
        );
        assertThat(resetResp.statusCode()).isEqualTo(200);

        // Old password fails, new password succeeds, reusing reset token fails
        assertThat(login(invitedEmail, "ValidPassword123").statusCode()).isEqualTo(401);
        assertThat(login(invitedEmail, "NewPassword456").statusCode()).isEqualTo(200);
        assertThat(postJson(
                "/api/auth/reset-password",
                """
                {"token":"%s","password":"AnotherPassword789"}
                """.formatted(resetToken),
                null
        ).statusCode()).isEqualTo(400);
    }

    @Test
    void forgotPassword_exceedingIpRateLimit_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> resp = postJson(
                    "/api/auth/forgot-password",
                    """
                    {"email":"unknown%d@test.local"}
                    """.formatted(i),
                    null,
                    "198.51.100.42"
            );
            assertThat(resp.statusCode()).isEqualTo(200);
        }

        HttpResponse<String> limited = postJson(
                "/api/auth/forgot-password",
                """
                {"email":"unknown6@test.local"}
                """,
                null,
                "198.51.100.42"
        );
        assertThat(limited.statusCode()).isEqualTo(429);
    }

    private String extractToken(EmailMessage message) {
        Matcher matcher = TOKEN_LINK_PATTERN.matcher(message.textBody());
        assertThat(matcher.find()).as("Expected token query param in email text body").isTrue();
        return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
    }

    private String loginForToken(String username, String password) throws Exception {
        HttpResponse<String> resp = login(username, password);
        assertThat(resp.statusCode()).isEqualTo(200);
        return mapper.readTree(resp.body()).get("token").asText();
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return postJson(
                "/api/auth/login",
                """
                {"username":"%s","password":"%s","portal":"CORE_ADMIN"}
                """.formatted(username, password),
                null
        );
    }

    private HttpResponse<String> postJson(String path, String json, String bearerToken) throws Exception {
        return postJson(path, json, bearerToken, null);
    }

    private HttpResponse<String> postJson(String path, String json, String bearerToken, String forwardedFor) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        if (forwardedFor != null) {
            builder.header("X-Forwarded-For", forwardedFor);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
