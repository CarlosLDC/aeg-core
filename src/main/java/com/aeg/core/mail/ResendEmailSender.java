package com.aeg.core.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResendEmailSender implements EmailSender {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final MailProperties mailProperties;
    private final ObjectMapper objectMapper;
    private volatile HttpClient httpClient;

    @Autowired
    public ResendEmailSender(
            MailProperties mailProperties,
            @Qualifier("mqttObjectMapper") ObjectMapper objectMapper) {
        this.mailProperties = mailProperties;
        this.objectMapper = objectMapper;
    }

    ResendEmailSender(MailProperties mailProperties, HttpClient httpClient) {
        this.mailProperties = mailProperties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
    }

    private HttpClient client() {
        HttpClient existing = this.httpClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (this.httpClient == null) {
                this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
            }
            return this.httpClient;
        }
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        String apiKey = mailProperties.getResend() != null ? mailProperties.getResend().getApiKey() : null;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn(
                    "[MAIL-FALLBACK] RESEND_API_KEY no configurada. Correo simulado -> to={}, subject={}",
                    message.to(),
                    message.subject()
            );
            return EmailSendResult.LOGGED_ONLY;
        }

        String apiUrl = mailProperties.getResend().getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://api.resend.com/emails";
        }

        String payload = serializePayload(message);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl.trim()))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                log.error("Resend devolvió HTTP {} al enviar correo a {}: {}", status, message.to(), response.body());
                throw new EmailDeliveryException("Resend respondió con estado HTTP " + status);
            }
            return EmailSendResult.SENT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailDeliveryException("Envío de correo interrumpido", e);
        } catch (IOException e) {
            log.error("Error de red al enviar correo vía Resend a {}", message.to(), e);
            throw new EmailDeliveryException("No se pudo conectar con Resend", e);
        }
    }

    private String serializePayload(EmailMessage message) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", mailProperties.getFrom());
            body.put("to", List.of(message.to()));
            body.put("subject", message.subject());
            body.put("html", message.htmlBody());
            body.put("text", message.textBody());
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new EmailDeliveryException("No se pudo serializar el cuerpo del correo", e);
        }
    }
}
