package com.aeg.core.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String from = "AEG Core <no-reply@aeg-admin.tech>";
    private String adminPortalUrl = "https://aeg-admin.tech";
    private String fiscalBookPortalUrl = "https://aeg-tech.com";
    private boolean asyncPasswordReset = true;
    private Resend resend = new Resend();

    public String normalizedAdminPortalUrl() {
        if (adminPortalUrl == null || adminPortalUrl.isBlank()) {
            return "https://aeg-admin.tech";
        }
        String trimmed = adminPortalUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Data
    public static class Resend {
        private String apiKey = "";
        private String apiUrl = "https://api.resend.com/emails";
    }
}
