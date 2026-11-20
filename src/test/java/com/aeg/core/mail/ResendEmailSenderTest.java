package com.aeg.core.mail;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResendEmailSenderTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void send_withoutApiKey_logsOnlyAndDoesNotCallHttp() {
        MailProperties props = new MailProperties();
        props.getResend().setApiKey("");
        ResendEmailSender sender = new ResendEmailSender(props, HttpClient.newHttpClient());

        EmailSendResult result = sender.send(new EmailMessage(
                "user@aeg.local",
                "Prueba",
                "<p>Hola</p>",
                "Hola"
        ));

        assertThat(result).isEqualTo(EmailSendResult.LOGGED_ONLY);
    }

    @Test
    void send_withApiKey_postsJsonWithBearerHeaderAndReturnsSent() throws IOException {
        AtomicReference<String> authHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"id\":\"email_123\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();

        MailProperties props = new MailProperties();
        props.setFrom("AEG Core <no-reply@aeg-admin.tech>");
        props.getResend().setApiKey("re_test_key");
        props.getResend().setApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/emails");

        ResendEmailSender sender = new ResendEmailSender(props, HttpClient.newHttpClient());
        EmailSendResult result = sender.send(new EmailMessage(
                "user@aeg.local",
                "Invitación",
                "<p>Activa tu cuenta</p>",
                "Activa tu cuenta"
        ));

        assertThat(result).isEqualTo(EmailSendResult.SENT);
        assertThat(authHeader.get()).isEqualTo("Bearer re_test_key");
        assertThat(requestBody.get())
                .contains("\"from\":\"AEG Core <no-reply@aeg-admin.tech>\"")
                .contains("\"to\":[\"user@aeg.local\"]")
                .contains("\"subject\":\"Invitación\"");
    }

    @Test
    void send_whenResendReturnsErrorStatus_throwsEmailDeliveryException() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            byte[] resp = "{\"message\":\"Invalid domain\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(422, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();

        MailProperties props = new MailProperties();
        props.getResend().setApiKey("re_test_key");
        props.getResend().setApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/emails");

        ResendEmailSender sender = new ResendEmailSender(props, HttpClient.newHttpClient());
        assertThatThrownBy(() -> sender.send(new EmailMessage(
                "user@aeg.local",
                "Invitación",
                "<p>Hola</p>",
                "Hola"
        ))).isInstanceOf(EmailDeliveryException.class);
    }
}
