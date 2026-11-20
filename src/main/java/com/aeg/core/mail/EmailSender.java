package com.aeg.core.mail;

public interface EmailSender {

    EmailSendResult send(EmailMessage message);
}
