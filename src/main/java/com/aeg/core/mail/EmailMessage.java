package com.aeg.core.mail;

public record EmailMessage(
        String to,
        String subject,
        String htmlBody,
        String textBody
) {
}
