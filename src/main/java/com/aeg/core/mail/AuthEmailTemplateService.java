package com.aeg.core.mail;

import com.aeg.core.security.Role;
import com.aeg.core.security.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthEmailTemplateService {

    private final MailProperties mailProperties;

    public String buildInvitationUrl(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return mailProperties.normalizedAdminPortalUrl() + "/accept-invite?token=" + encodedToken;
    }

    public String buildPasswordResetUrl(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return mailProperties.normalizedAdminPortalUrl() + "/reset-password?token=" + encodedToken;
    }

    public EmailMessage buildInvitationEmail(User user, String rawToken, long expirationHours) {
        String acceptUrl = buildInvitationUrl(rawToken);
        String displayName = safeDisplayName(user);
        String roleLabel = roleLabel(user.getRole());
        String expirationLabel = formatHours(expirationHours);

        String subject = "Invitación a AEG Core — Activa tu cuenta";

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8" /></head>
                <body style="margin:0;padding:24px;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#18181b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;border:1px solid #e4e4e7;overflow:hidden;">
                    <tr>
                      <td style="padding:28px 32px;background:#0f172a;color:#ffffff;">
                        <p style="margin:0;font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:#94a3b8;">AEG Core</p>
                        <h1 style="margin:8px 0 0;font-size:20px;font-weight:600;">Has sido invitado al sistema</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:28px 32px;font-size:15px;line-height:1.6;">
                        <p style="margin:0 0 16px;">Hola <strong>%s</strong>,</p>
                        <p style="margin:0 0 16px;">Se ha creado una cuenta para ti en la plataforma AEG con el rol de <strong>%s</strong>.</p>
                        <p style="margin:0 0 24px;">Para activar tu cuenta y definir tu contraseña, haz clic en el siguiente botón:</p>
                        <p style="margin:0 0 24px;text-align:center;">
                          <a href="%s" style="display:inline-block;padding:12px 24px;background:#0284c7;color:#ffffff;text-decoration:none;font-weight:600;border-radius:8px;">Activar cuenta y definir contraseña</a>
                        </p>
                        <p style="margin:0 0 12px;font-size:13px;color:#52525b;">Este enlace es de un solo uso y expirará en <strong>%s</strong>.</p>
                        <p style="margin:0 0 8px;font-size:13px;color:#52525b;">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                        <p style="margin:0;font-size:12px;word-break:break-all;color:#0284c7;">%s</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 32px;background:#fafafa;border-top:1px solid #e4e4e7;font-size:12px;color:#71717a;">
                        Si no esperabas esta invitación, puedes ignorar este correo.
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(displayName),
                escapeHtml(roleLabel),
                escapeHtml(acceptUrl),
                escapeHtml(expirationLabel),
                escapeHtml(acceptUrl)
        );

        String text = """
                Hola %s,

                Se ha creado una cuenta para ti en AEG Core con el rol de %s.
                Para activar tu cuenta y definir tu contraseña, abre el siguiente enlace (válido por %s, de un solo uso):

                %s

                Si no esperabas esta invitación, puedes ignorar este correo.
                """.formatted(displayName, roleLabel, expirationLabel, acceptUrl);

        return new EmailMessage(user.getUsername(), subject, html, text);
    }

    public EmailMessage buildPasswordResetEmail(User user, String rawToken, long expirationMinutes) {
        String resetUrl = buildPasswordResetUrl(rawToken);
        String displayName = safeDisplayName(user);
        String expirationLabel = formatMinutes(expirationMinutes);

        String subject = "AEG Core — Restablece tu contraseña";

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8" /></head>
                <body style="margin:0;padding:24px;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#18181b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;border:1px solid #e4e4e7;overflow:hidden;">
                    <tr>
                      <td style="padding:28px 32px;background:#0f172a;color:#ffffff;">
                        <p style="margin:0;font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:#94a3b8;">AEG Core</p>
                        <h1 style="margin:8px 0 0;font-size:20px;font-weight:600;">Restablecimiento de contraseña</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:28px 32px;font-size:15px;line-height:1.6;">
                        <p style="margin:0 0 16px;">Hola <strong>%s</strong>,</p>
                        <p style="margin:0 0 24px;">Recibimos una solicitud para restablecer la contraseña de tu cuenta. Haz clic en el botón para elegir una nueva contraseña:</p>
                        <p style="margin:0 0 24px;text-align:center;">
                          <a href="%s" style="display:inline-block;padding:12px 24px;background:#0284c7;color:#ffffff;text-decoration:none;font-weight:600;border-radius:8px;">Restablecer contraseña</a>
                        </p>
                        <p style="margin:0 0 12px;font-size:13px;color:#52525b;">Este enlace es de un solo uso y expirará en <strong>%s</strong>.</p>
                        <p style="margin:0 0 8px;font-size:13px;color:#52525b;">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                        <p style="margin:0;font-size:12px;word-break:break-all;color:#0284c7;">%s</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 32px;background:#fafafa;border-top:1px solid #e4e4e7;font-size:12px;color:#71717a;">
                        Si no solicitaste este cambio, puedes ignorar este correo de forma segura; tu contraseña actual no cambiará.
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(displayName),
                escapeHtml(resetUrl),
                escapeHtml(expirationLabel),
                escapeHtml(resetUrl)
        );

        String text = """
                Hola %s,

                Recibimos una solicitud para restablecer la contraseña de tu cuenta en AEG Core.
                Abre el siguiente enlace para definir una nueva contraseña (válido por %s, de un solo uso):

                %s

                Si no solicitaste este cambio, puedes ignorar este correo.
                """.formatted(displayName, expirationLabel, resetUrl);

        return new EmailMessage(user.getUsername(), subject, html, text);
    }

    private static String safeDisplayName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        return user.getUsername();
    }

    private static String roleLabel(Role role) {
        if (role == null) {
            return "Usuario";
        }
        return switch (role) {
            case ADMIN -> "Administrador";
            case DISTRIBUTOR -> "Distribuidor";
            case TECHNICIAN, SERVICE_CENTER -> "Técnico de centro de servicio";
            case SENIAT -> "Auditor SENIAT";
            case FINANCE -> "Finanzas";
        };
    }

    private static String formatHours(long hours) {
        if (hours == 1) {
            return "1 hora";
        }
        return hours + " horas";
    }

    private static String formatMinutes(long minutes) {
        if (minutes == 1) {
            return "1 minuto";
        }
        return minutes + " minutos";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
